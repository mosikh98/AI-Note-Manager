package com.ainotes.app.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromBool(value: Boolean): Int = if (value) 1 else 0
    @TypeConverter fun toBool(value: Int): Boolean = value != 0
}
