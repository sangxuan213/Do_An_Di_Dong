package com.example.uthdocument.data.office

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

internal fun newXmlParser(input: InputStream): XmlPullParser =
    Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        setInput(input, null)
    }
