package com.xmartlabs.slackbot.extensions

import com.slack.api.model.block.InputBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.model.block.element.ChannelsSelectElement
import com.slack.api.model.block.element.MultiUsersSelectElement
import com.slack.api.model.block.element.PlainTextInputElement
import com.slack.api.model.block.element.StaticSelectElement
import com.slack.api.model.block.element.UsersSelectElement
import com.slack.api.model.view.ViewState

fun MutableMap<String, MutableMap<String, ViewState.Value>>.getPlainTextValue(input: InputBlock) =
    this[input.blockId]?.get((input.element as PlainTextInputElement).actionId)?.value

fun MutableMap<String, MutableMap<String, ViewState.Value>>.getSelectedUser(input: InputBlock) =
    this[input.blockId]?.get((input.element as UsersSelectElement).actionId)?.selectedUser

fun MutableMap<String, MutableMap<String, ViewState.Value>>.getSelectedUsers(input: InputBlock) =
    this[input.blockId]?.get((input.element as MultiUsersSelectElement).actionId)?.selectedUsers

fun MutableMap<String, MutableMap<String, ViewState.Value>>.getSelectedChannel(input: InputBlock) =
    this[input.blockId]?.get((input.element as ChannelsSelectElement).actionId)?.selectedChannel

fun MutableMap<String, MutableMap<String, ViewState.Value>>.getSelectedOptionValue(input: SectionBlock) =
    this[input.blockId]?.get((input.accessory as StaticSelectElement).actionId)?.selectedOption?.value
