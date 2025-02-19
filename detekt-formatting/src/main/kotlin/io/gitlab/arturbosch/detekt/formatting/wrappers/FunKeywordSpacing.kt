package io.gitlab.arturbosch.detekt.formatting.wrappers

import com.pinterest.ktlint.ruleset.experimental.FunKeywordSpacingRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.internal.AutoCorrectable
import io.gitlab.arturbosch.detekt.formatting.FormattingRule

/**
 * See <a href="https://ktlint.github.io/#rule-spacing">ktlint-website</a> for documentation.
 */
@AutoCorrectable(since = "1.20.0")
class FunKeywordSpacing(config: Config) : FormattingRule(config) {

    override val wrapping = FunKeywordSpacingRule()
    override val issue = issueFor("Checks the spacing after the fun keyword.")
}
