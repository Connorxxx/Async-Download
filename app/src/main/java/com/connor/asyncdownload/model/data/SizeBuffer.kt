package com.connor.asyncdownload.model.data

import android.animation.ObjectAnimator
import kotlinx.coroutines.Job

data class DownJob(
    override val id: Int,
    val job: Job
): ID

data class Animator(
    override val id: Int,
    val anima: ObjectAnimator
): ID

sealed interface ID {
    val id: Int
}
