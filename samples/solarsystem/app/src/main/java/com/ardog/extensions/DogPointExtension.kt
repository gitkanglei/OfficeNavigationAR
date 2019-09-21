package com.ardog.extensions

import android.text.TextUtils
import com.ardog.model.DogPoint
import java.util.*

fun DogPoint.getAdjacentPointList() : List<Long> {
    if (TextUtils.isEmpty(ids)) {
        return LinkedList()
    }
    return ids.split(",".toRegex())
            .dropLastWhile { it.isEmpty() }
            .filter { !TextUtils.isEmpty(it) }
            .map { it.toLong() }
            .toList()
}