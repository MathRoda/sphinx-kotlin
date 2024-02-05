package chat.sphinx.activitymain.fake

import chat.sphinx.concept_user_colors_helper.UserColorsHelper

class FakeUserColorHelper : UserColorsHelper() {
    override suspend fun getHexCodeForKey(colorKey: String, randomHexColorCode: String): String {
        return ""
    }
}