package com.benjaminearley.droidbot

infix inline fun <P1, R> P1.pipe(t: (P1) -> R): R = t(this)

