package com.pandakind.prompteditor.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Profile(
    @JsonProperty("name") val name: String = "",
    @JsonProperty("profileData") val profileData: Map<String, String> = emptyMap(),
    @JsonProperty("templatesData") val templatesData: Map<String, List<String>> = emptyMap()
)