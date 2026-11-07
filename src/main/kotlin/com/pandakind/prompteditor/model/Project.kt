package com.pandakind.prompteditor.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Project(
    @JsonProperty("name") val name: String,
    @JsonProperty("lastPrompt") val lastPrompt: String? = null,
    @JsonProperty("activeTemplate") val activeTemplate: String? = null
)
