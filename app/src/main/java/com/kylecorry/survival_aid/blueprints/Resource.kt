package com.kylecorry.survival_aid.blueprints

/**
 * A resource
 * @param name the name of the resource
 * @param description the description of the resource
 * @param imageResourceId the image resource ID
 */
data class Resource(val name: String, val description: String, val imageResourceId: Int? = null)