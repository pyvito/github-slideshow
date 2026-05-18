package com.wled.manager.model

import com.google.gson.annotations.SerializedName

data class WledState(
    @SerializedName("on") val on: Boolean = false,
    @SerializedName("bri") val brightness: Int = 128,
    @SerializedName("seg") val segments: List<Segment> = emptyList(),
    @SerializedName("ps") val presetId: Int = -1,
    @SerializedName("pl") val playlistId: Int = -1
)

data class Segment(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("fx") val effect: Int = 0,
    @SerializedName("pal") val palette: Int = 0,
    @SerializedName("col") val colors: List<List<Int>> = listOf(listOf(255, 160, 0)),
    @SerializedName("sx") val speed: Int = 128,
    @SerializedName("ix") val intensity: Int = 128
)

data class WledInfo(
    @SerializedName("name") val name: String = "",
    @SerializedName("ver") val version: String = "",
    @SerializedName("leds") val leds: LedsInfo = LedsInfo()
)

data class LedsInfo(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("rgbw") val rgbw: Boolean = false
)

data class StateCommand(
    @SerializedName("on") val on: Boolean? = null,
    @SerializedName("bri") val brightness: Int? = null,
    @SerializedName("seg") val segments: List<SegmentCommand>? = null,
    @SerializedName("ps") val presetId: Int? = null
)

data class SegmentCommand(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("fx") val effect: Int? = null,
    @SerializedName("pal") val palette: Int? = null,
    @SerializedName("col") val colors: List<List<Int>>? = null,
    @SerializedName("sx") val speed: Int? = null,
    @SerializedName("ix") val intensity: Int? = null
)
