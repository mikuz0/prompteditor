package com.pandakind.prompteditor.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.LinkedHashMap

data class Template(
    @JsonProperty("name") val name: String,
    @JsonProperty("sections") val sections: List<Section> = emptyList(),
    @JsonProperty("templates") val templates: LinkedHashMap<String, List<String>> = LinkedHashMap()
)
