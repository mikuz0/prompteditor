package com.pandakind.prompteditor.controller

import com.pandakind.prompteditor.model.Profile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions

class ProfileControllerTest {

    @TempDir
    lateinit var tempDir: Path

    private fun createProfile(name: String, data: Map<String, String>, templates: Map<String, List<String>> = emptyMap()): Profile {
        return Profile(
            name = name,
            profileData = data,
            templatesData = templates
        )
    }

    private fun controller(): ProfileController {
        return ProfileController(tempDir)
    }

    @Test
    fun `should save and load profile round-trip`() {
        val data = mapOf(
            "role" to "You are an assistant",
            "goal" to "Help the user"
        )
        val templates = mapOf(
            "role" to listOf("You are a helper", "Act as an expert")
        )
        val profile = createProfile("test_profile", data, templates)

        val ctrl = controller()
        ctrl.saveProfile(profile)

        val loaded = ctrl.loadProfile("test_profile")
        Assertions.assertNotNull(loaded)
        assertEquals("test_profile", loaded?.name)
        assertEquals(data, loaded?.profileData)
        assertEquals(templates, loaded?.templatesData)
    }

    @Test
    fun `should list saved profiles`() {
        val ctrl = controller()

        ctrl.saveProfile(createProfile("profile_a", mapOf("key" to "value_a")))
        ctrl.saveProfile(createProfile("profile_b", mapOf("key" to "value_b")))

        val profiles = ctrl.listProfiles()
        assertEquals(2, profiles.size)
        assertEquals("profile_a", profiles[0])
        assertEquals("profile_b", profiles[1])
    }

    @Test
    fun `should delete profile`() {
        val ctrl = controller()

        ctrl.saveProfile(createProfile("to_delete", mapOf("key" to "value")))
        assertEquals(1, ctrl.listProfiles().size)

        val deleted = ctrl.deleteProfile("to_delete")
        Assertions.assertTrue(deleted)
        assertEquals(0, ctrl.listProfiles().size)
        assertNull(ctrl.loadProfile("to_delete"))
    }

    @Test
    fun `should return false for deleting non-existent profile`() {
        val ctrl = controller()
        val deleted = ctrl.deleteProfile("non_existent")
        Assertions.assertFalse(deleted)
    }

    @Test
    fun `should save and load last profile`() {
        val ctrl = controller()

        ctrl.saveProfile(createProfile("last_one", mapOf("key" to "value")))

        val last = ctrl.loadLastProfile()
        assertEquals("last_one", last)
    }

    @Test
    fun `should return null for last profile when none saved`() {
        val ctrl = controller()
        assertNull(ctrl.loadLastProfile())
    }

    @Test
    fun `should update last profile when saving new one`() {
        val ctrl = controller()

        ctrl.saveProfile(createProfile("first", mapOf("key" to "v1")))
        ctrl.saveProfile(createProfile("second", mapOf("key" to "v2")))

        val last = ctrl.loadLastProfile()
        assertEquals("second", last)
    }

    @Test
    fun `should save profile with empty templates`() {
        val ctrl = controller()
        val profile = createProfile("no_templates", mapOf("key" to "value"))

        ctrl.saveProfile(profile)

        val loaded = ctrl.loadProfile("no_templates")
        Assertions.assertNotNull(loaded)
        assertEquals(emptyMap<String, List<String>>(), loaded?.templatesData)
    }

    @Test
    fun `should save as new profile name`() {
        val ctrl = controller()

        ctrl.saveProfile(createProfile("original", mapOf("key" to "value")))
        ctrl.saveProfileAs(
            ctrl.loadProfile("original")!!,
            "copy"
        )

        val profiles = ctrl.listProfiles()
        assertEquals(2, profiles.size)
        assertEquals("copy", profiles[0])
        assertEquals("original", profiles[1])

        val copy = ctrl.loadProfile("copy")
        Assertions.assertNotNull(copy)
        assertEquals("copy", copy?.name)
    }

    @Test
    fun `should export and import json`() {
        val ctrl = controller()
        val profile = createProfile("json_test", mapOf("a" to "1", "b" to "2"))

        val json = ctrl.exportToJson(profile)
        val imported = ctrl.importFromJson(json)

        assertEquals(profile.name, imported.name)
        assertEquals(profile.profileData, imported.profileData)
        assertEquals(profile.templatesData, imported.templatesData)
    }

    @Test
    fun `should clean last profile when deleting it`() {
        val ctrl = controller()

        ctrl.saveProfile(createProfile("deletable", mapOf("key" to "value")))
        assertEquals("deletable", ctrl.loadLastProfile())

        ctrl.deleteProfile("deletable")
        assertNull(ctrl.loadLastProfile())
    }
}
