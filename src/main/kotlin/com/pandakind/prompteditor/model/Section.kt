package com.pandakind.prompteditor.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Section(
    @JsonProperty("key") val key: String,
    @JsonProperty("title") val title: String,
    @JsonProperty("icon") val icon: String = "",
    @JsonProperty("description") val description: String = "",
    @JsonProperty("defaultText") val defaultText: String = "",
    @JsonProperty("order") val order: Int = 0,
    @JsonProperty("enabled") val enabled: Boolean = true
)