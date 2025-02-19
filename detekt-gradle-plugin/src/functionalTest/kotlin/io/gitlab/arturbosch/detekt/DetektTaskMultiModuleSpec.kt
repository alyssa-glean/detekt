package io.gitlab.arturbosch.detekt

import io.gitlab.arturbosch.detekt.testkit.DslGradleRunner
import io.gitlab.arturbosch.detekt.testkit.DslTestBuilder
import io.gitlab.arturbosch.detekt.testkit.ProjectLayout
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DetektTaskMultiModuleSpec {

    @Nested
    inner class `The Detekt Gradle plugin used in a multi module project` {

        @Test
        @DisplayName(
            "it is applied with defaults to all subprojects individually without " +
                "sources in root project using the subprojects block"
        )
        fun applyToSubprojectsWithoutSources() {
            val projectLayout = ProjectLayout(0).apply {
                addSubmodule("child1", 2)
                addSubmodule("child2", 4)
            }

            val builder = DslTestBuilder.kotlin()

            val mainBuildFileContent: String = """
                        |${builder.gradlePlugins}
                        |
                        |allprojects {
                        |   ${builder.gradleRepositories}
                        |}
                        |subprojects {
                        |   ${builder.gradleSubprojectsApplyPlugins}
                        |}
                        |
            """.trimMargin()

            val gradleRunner = DslGradleRunner(projectLayout, builder.gradleBuildName, mainBuildFileContent)

            gradleRunner.setupProject()
            gradleRunner.runDetektTaskAndCheckResult { result ->
                assertThat(result.task(":detekt")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
                projectLayout.submodules.forEach { submodule ->
                    assertThat(result.task(":${submodule.name}:detekt")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                }

                assertThat(projectFile("build/reports/detekt/detekt.xml")).doesNotExist()
                assertThat(projectFile("build/reports/detekt/detekt.html")).doesNotExist()
                assertThat(projectFile("build/reports/detekt/detekt.txt")).doesNotExist()
                projectLayout.submodules.forEach {
                    assertThat(projectFile("${it.name}/build/reports/detekt/detekt.xml")).exists()
                    assertThat(projectFile("${it.name}/build/reports/detekt/detekt.html")).exists()
                    assertThat(projectFile("${it.name}/build/reports/detekt/detekt.txt")).exists()
                }
            }
        }

        @Test
        @DisplayName(
            "it is applied with defaults to main project and subprojects " +
                "individually using the allprojects block"
        )
        fun applyWithAllprojectsBlock() {
            val projectLayout = ProjectLayout(1).apply {
                addSubmodule("child1", 2)
                addSubmodule("child2", 4)
            }

            val builder = DslTestBuilder.kotlin()

            val mainBuildFileContent: String = """
                        |${builder.gradlePlugins}
                        |
                        |allprojects {
                        |   ${builder.gradleRepositories}
                        |   ${builder.gradleSubprojectsApplyPlugins}
                        |}
                        |
            """.trimMargin()

            val gradleRunner = DslGradleRunner(projectLayout, builder.gradleBuildName, mainBuildFileContent)

            gradleRunner.setupProject()
            gradleRunner.runDetektTaskAndCheckResult { result ->
                assertThat(result.task(":detekt")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                projectLayout.submodules.forEach { submodule ->
                    assertThat(result.task(":${submodule.name}:detekt")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                }

                assertThat(projectFile("build/reports/detekt/detekt.xml")).exists()
                assertThat(projectFile("build/reports/detekt/detekt.html")).exists()
                assertThat(projectFile("build/reports/detekt/detekt.txt")).exists()
                projectLayout.submodules.forEach {
                    assertThat(projectFile("${it.name}/build/reports/detekt/detekt.xml")).exists()
                    assertThat(projectFile("${it.name}/build/reports/detekt/detekt.html")).exists()
                    assertThat(projectFile("${it.name}/build/reports/detekt/detekt.txt")).exists()
                }
            }
        }

        @Test
        fun `it uses custom configs when configured in allprojects block`() {
            val projectLayout = ProjectLayout(1).apply {
                addSubmodule("child1", 2)
                addSubmodule("child2", 4)
            }

            val builder = DslTestBuilder.kotlin()

            val mainBuildFileContent: String = """
                        |${builder.gradlePlugins}
                        |
                        |allprojects {
                        |   ${builder.gradleRepositories}
                        |   ${builder.gradleSubprojectsApplyPlugins}
                        |
                        |   detekt {
                        |       reportsDir = file("build/detekt-reports")
                        |   }
                        |}
                        |
            """.trimMargin()

            val gradleRunner = DslGradleRunner(projectLayout, builder.gradleBuildName, mainBuildFileContent)
            gradleRunner.setupProject()
            gradleRunner.runDetektTaskAndCheckResult { result ->
                assertThat(result.task(":detekt")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                projectLayout.submodules.forEach { submodule ->
                    assertThat(result.task(":${submodule.name}:detekt")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                }

                assertThat(projectFile("build/detekt-reports/detekt.xml")).exists()
                assertThat(projectFile("build/detekt-reports/detekt.html")).exists()
                assertThat(projectFile("build/detekt-reports/detekt.txt")).exists()
                projectLayout.submodules.forEach {
                    assertThat(projectFile("${it.name}/build/detekt-reports/detekt.xml")).exists()
                    assertThat(projectFile("${it.name}/build/detekt-reports/detekt.html")).exists()
                    assertThat(projectFile("${it.name}/build/detekt-reports/detekt.txt")).exists()
                }
            }
        }

        @Test
        @DisplayName("it allows changing defaults in allprojects block that can be overwritten in subprojects")
        fun allowsChangingDefaultsInAllProjectsThatAreOverwrittenInSubprojects() {
            val child2DetektConfig = """
                        |detekt {
                        |   reportsDir = file("build/custom")
                        |}
                        |
            """.trimMargin()

            val projectLayout = ProjectLayout(1).apply {
                addSubmodule("child1", 2)
                addSubmodule("child2", 4, buildFileContent = child2DetektConfig)
            }

            val builder = DslTestBuilder.kotlin()

            val mainBuildFileContent: String = """
                        |${builder.gradlePlugins}
                        |
                        |allprojects {
                        |   ${builder.gradleRepositories}
                        |   ${builder.gradleSubprojectsApplyPlugins}
                        |
                        |   detekt {
                        |       reportsDir = file("build/detekt-reports")
                        |   }
                        |}
                        |
            """.trimMargin()

            val gradleRunner = DslGradleRunner(projectLayout, builder.gradleBuildName, mainBuildFileContent)

            gradleRunner.setupProject()
            gradleRunner.runDetektTaskAndCheckResult { result ->
                assertThat(result.task(":detekt")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                projectLayout.submodules.forEach { submodule ->
                    assertThat(result.task(":${submodule.name}:detekt")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                }

                assertThat(projectFile("build/detekt-reports/detekt.xml")).exists()
                assertThat(projectFile("build/detekt-reports/detekt.html")).exists()
                assertThat(projectFile("build/detekt-reports/detekt.txt")).exists()
                assertThat(projectFile("child1/build/detekt-reports/detekt.xml")).exists()
                assertThat(projectFile("child1/build/detekt-reports/detekt.html")).exists()
                assertThat(projectFile("child1/build/detekt-reports/detekt.txt")).exists()
                assertThat(projectFile("child2/build/custom/detekt.xml")).exists()
                assertThat(projectFile("child2/build/custom/detekt.html")).exists()
                assertThat(projectFile("child2/build/custom/detekt.txt")).exists()
            }
        }

        @Test
        fun `it can be applied to all files in entire project resulting in 1 report`() {
            val projectLayout = ProjectLayout(1).apply {
                addSubmodule("child1", 2)
                addSubmodule("child2", 4)
            }

            val detektConfig: String = """
                        |detekt {
                        |    source = files(
                        |       "${"$"}projectDir/src",
                        |       "${"$"}projectDir/child1/src",
                        |       "${"$"}projectDir/child2/src"
                        |    )
                        |}
            """.trimMargin()
            val gradleRunner = DslTestBuilder.kotlin()
                .withProjectLayout(projectLayout)
                .withDetektConfig(detektConfig)
                .build()

            gradleRunner.runDetektTaskAndCheckResult { result ->
                assertThat(result.task(":detekt")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
                projectLayout.submodules.forEach { submodule ->
                    assertThat(result.task(":${submodule.name}:detekt")).isNull()
                }

                assertThat(projectFile("build/reports/detekt/detekt.xml")).exists()
                assertThat(projectFile("build/reports/detekt/detekt.html")).exists()
                assertThat(projectFile("build/reports/detekt/detekt.txt")).exists()
                projectLayout.submodules.forEach { submodule ->
                    assertThat(projectFile("${submodule.name}/build/reports/detekt/detekt.xml")).doesNotExist()
                    assertThat(projectFile("${submodule.name}/build/reports/detekt/detekt.html")).doesNotExist()
                    assertThat(projectFile("${submodule.name}/build/reports/detekt/detekt.txt")).doesNotExist()
                }
            }
        }
    }
}
