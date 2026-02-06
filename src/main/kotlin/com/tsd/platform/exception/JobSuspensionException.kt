package com.tsd.platform.exception

class JobSuspensionException(
    val reason: String,
    val suspenseCode: String
) : RuntimeException(reason)