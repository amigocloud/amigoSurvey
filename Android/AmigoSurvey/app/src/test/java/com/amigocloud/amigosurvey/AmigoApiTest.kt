package com.amigocloud.amigosurvey

import android.content.SharedPreferences
import com.amigocloud.amigosurvey.models.AmigoToken
import com.amigocloud.amigosurvey.repository.*
import com.amigocloud.amigosurvey.util.save
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import io.reactivex.Single
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.junit.MockitoRule
import retrofit2.Retrofit
import toothpick.Toothpick
import toothpick.testing.ToothPickRule
import javax.inject.Inject


@RunWith(MockitoJUnitRunner::class)
class AmigoApiTest {

    @Rule @JvmField val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @Rule @JvmField val toothPickRule = ToothPickRule(this)

    @Mock lateinit var prefs: SharedPreferences
    @Mock lateinit var retrofit: Retrofit
    @Mock lateinit var api: AmigoApi

    @Inject lateinit var config: SurveyConfig
    @Inject lateinit var rest: AmigoRest

    @Before
    fun setup() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        `when`(retrofit.create(AmigoApi::class.java)).thenReturn(api)
        toothPickRule.testModule.bind(Moshi::class.java).toInstance(moshi)
        toothPickRule.setScopeName(ApplicationScope::class.java)
        toothPickRule.inject(this)
    }

    @After
    fun tearDown() {
        Toothpick.reset()
    }

    @Test
    fun testInjections() {
        assertNotNull(config)
        assertNotNull(rest)
//        `when`(prefs.getLong("test", 0)).thenReturn(1)
//        assertEquals(1, config.getLong("test"))
    }

    @Test
    fun testLogin() {
        val token = AmigoToken(access_token = "token")

        `when`(api.login(
                client_id = AmigoClient.client_id,
                client_secret = AmigoClient.client_secret,
                grant_type = "password",
                username = "user",
                password = "password")
        ).thenReturn(Single.just(token))

        val observer = rest.login("user", "password").test()
        observer.assertComplete()
    }

    @Test
    fun testSetLong() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putLong(anyString(), anyLong())).thenReturn(editor)

        val l = 100L
        l.save(prefs, "test")

        verify(editor, times(1)).putLong("test", 100)
        verify(editor, times(1)).apply()
    }
}
