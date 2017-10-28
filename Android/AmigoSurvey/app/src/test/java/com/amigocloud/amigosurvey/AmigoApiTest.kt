package com.amigocloud.amigosurvey

import android.content.SharedPreferences
import com.amigocloud.amigosurvey.models.AmigoToken
import com.amigocloud.amigosurvey.models.UserModel
import com.amigocloud.amigosurvey.repository.*
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import io.reactivex.Single
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.*
import toothpick.Toothpick
import toothpick.testing.ToothPickRule
import javax.inject.Inject
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.HttpException
import retrofit2.Response


@RunWith(MockitoJUnitRunner::class)
class AmigoApiTest {

    @Rule @JvmField val mockitoRule = MockitoJUnit.rule()
    @Rule @JvmField val toothPickRule = ToothPickRule(this)

    @Mock lateinit var prefs: SharedPreferences
    @Mock lateinit var api: AmigoApi

    @Inject lateinit var config: SurveyConfig
    @Inject lateinit var rest: AmigoRest
    @Inject lateinit var repo: Repository

    @Before
    fun setup() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
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
        assertNotNull(repo)
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
        observer.assertValue(token)
    }

    @Test
    fun testSetLong() {
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putLong(anyString(), anyLong())).thenReturn(editor)
        config.setLong(100, "test")
        verify(editor, times(1)).putLong("test", 100)
        verify(editor, times(1)).apply()
    }

    @Test
    fun testRefreshToken() {
        val user = UserModel(email = "email")
        val token = AmigoToken(access_token = "token")
        val refreshToken = AmigoToken(access_token = "refresh_token")

        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(api.login(anyString(),anyString(), anyString(), anyString(), anyString())).thenReturn(Single.just(token))
        `when`(prefs.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)

        val observer = rest.login("", "").test()
        observer.assertValue(token)

        val error = HttpException(
                Response.error<UserModel>(403,
                        ResponseBody.create(MediaType.parse(""), "")))
        `when`(api.getUser(anyString())).thenReturn(Single.error(error), Single.just(user))
        `when`(api.refreshToken(anyString(),anyString(), anyString(), anyString())).thenReturn(Single.just(refreshToken))

        val userObserver = rest.fetchUser().test()
        userObserver.assertValue(user)
    }
}
