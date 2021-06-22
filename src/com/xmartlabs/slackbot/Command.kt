package com.xmartlabs.slackbot

class Command(vararg val keys: String, private val answerCallback: () -> String) {
    val answer: String
        get() = answerCallback()
}
