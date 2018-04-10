/*
 * Copyright (c) 2017-2018 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package com.here.ort.model

import com.here.ort.utils.normalizedPath

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.WordSpec

import java.io.File

class MergedAnalyzerResultTest : WordSpec() {

    private val repositoryPath = File("/absolute/path")
    private val directoryDetails = Repository(repositoryPath.name, repositoryPath.normalizedPath, VcsInfo.EMPTY)

    private val package1 = Package.EMPTY.copy(id = Identifier("provider-1", "namespace-1", "package-1", "version-1"))
    private val package2 = Package.EMPTY.copy(id = Identifier("provider-2", "namespace-2", "package-2", "version-2"))
    private val package3 = Package.EMPTY.copy(id = Identifier("provider-3", "namespace-3", "package-3", "version-3"))

    private val pkgRef1 = package1.toReference()
    private val pkgRef2 = package2.toReference(sortedSetOf(package3.toReference()))

    private val scope1 = Scope("scope-1", true, sortedSetOf(pkgRef1))
    private val scope2 = Scope("scope-2", true, sortedSetOf(pkgRef2))

    private val project1 = Project.EMPTY.copy(
            id = Identifier("provider-1", "namespace-1", "project-1", "version-1"),
            scopes = sortedSetOf(scope1)
    )
    private val project2 = Project.EMPTY.copy(
            id = Identifier("provider-2", "namespace-2", "project-2", "version-2"),
            scopes = sortedSetOf(scope1, scope2)
    )

    private val analyzerResult1 = AnalyzerResult(true, project1, sortedSetOf(package1), listOf("error-1", "error-2"))
    private val analyzerResult2 =
            AnalyzerResult(true, project2, sortedSetOf(package1, package2, package3), listOf("error-2"))

    init {
        "MergedAnalyzerResult" should {
            "create the correct AnalyzerResults" {
                val builder = MergedResultsBuilder(true, repositoryPath, VcsInfo.EMPTY)

                builder.addResult("/analyzer-result-1.yml", analyzerResult1)
                builder.addResult("/analyzer-result-2.yml", analyzerResult2)

                val mergedResults = builder.build()

                mergedResults.createAnalyzerResults() shouldBe listOf(analyzerResult1, analyzerResult2)
            }

            "can be serialized and deserialized" {
                val builder = MergedResultsBuilder(true, repositoryPath, VcsInfo.EMPTY)

                builder.addResult("/analyzer-result-1.yml", analyzerResult1)
                builder.addResult("/analyzer-result-2.yml", analyzerResult2)

                val mergedResults = builder.build()

                val serializedMergedResults = yamlMapper.writeValueAsString(mergedResults)
                val deserializedMergedResults =
                        yamlMapper.readValue(serializedMergedResults, MergedAnalyzerResult::class.java)

                deserializedMergedResults shouldBe mergedResults
            }
        }

        "MergedResultsBuilder" should {
            "merge results from all files" {
                val builder = MergedResultsBuilder(true, repositoryPath, VcsInfo.EMPTY)

                builder.addResult("/analyzer-result-1.yml", analyzerResult1)
                builder.addResult("/analyzer-result-2.yml", analyzerResult2)

                val mergedResults = builder.build()

                mergedResults.allowDynamicVersions shouldBe true
                mergedResults.repository shouldBe directoryDetails
                mergedResults.projects shouldBe sortedSetOf(project1, project2)
                mergedResults.projectResultsFiles shouldBe
                        sortedMapOf(project1.id to "/analyzer-result-1.yml", project2.id to "/analyzer-result-2.yml")
                mergedResults.packages shouldBe sortedSetOf(package1, package2, package3)
                mergedResults.errors shouldBe
                        sortedMapOf(project1.id to analyzerResult1.errors, project2.id to analyzerResult2.errors)
            }
        }
    }
}
