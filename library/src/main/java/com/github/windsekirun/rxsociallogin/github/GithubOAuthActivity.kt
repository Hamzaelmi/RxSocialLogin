package com.github.windsekirun.rxsociallogin.github

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.github.windsekirun.rxsociallogin.R
import com.github.windsekirun.rxsociallogin.SocialLogin
import com.github.windsekirun.rxsociallogin.intenal.OkHttpHelper
import com.github.windsekirun.rxsociallogin.model.SocialType
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_github.*
import okhttp3.HttpUrl

/**
 * RxSocialLogin
 * Class: GithubOAuthActivity
 * Created by pyxis on 18. 7. 27.
 *
 * Description:
 */
class GithubOAuthActivity : AppCompatActivity() {
    lateinit var githubConfig: GithubConfig
    lateinit var disposable: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_github)

        githubConfig = SocialLogin.getConfig(SocialType.GITHUB) as GithubConfig

        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::disposable.isInitialized && !disposable.isDisposed) {
            disposable.dispose()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finishActivity()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun init() {
        var githubUrl = "${GithubOAuthConstants.GITHUB_URL} + ?client_id=${githubConfig.clientId}"

        if (githubConfig.scopeList.isNotEmpty()) {
            val scope = githubConfig.scopeList.joinToString(",")
            githubUrl += scope
        }

        if (githubConfig.clearCookies) {
            val cookieManager = CookieManager.getInstance()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.removeAllCookies { }
            } else {
                cookieManager.removeAllCookie()
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, uri: String?): Boolean {
                super.shouldOverrideUrlLoading(view, uri)

                try {
                    if (uri?.contains("?code=") == false) return false
                    val url = uri!!

                    val githubCode = url.substring(url.lastIndexOf("?code") + 1)
                    val tokenCode = githubCode.split("=")
                    val tokenFetchedIds = tokenCode[1]
                    val cleanToken = tokenFetchedIds.split("&")

                    requestOAuthToken(cleanToken[0])
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return false
            }
        }

        setSupportActionBar(toolBar)
        supportActionBar?.let {
            it.title = githubConfig.activityTitle
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun requestOAuthToken(code: String) {
        val urlBuilder = HttpUrl.parse(GithubOAuthConstants.GITHUB_OAUTH)!!.newBuilder()
        urlBuilder.addQueryParameter("client_id", githubConfig.clientId)
        urlBuilder.addQueryParameter("client_secret", githubConfig.clientSecret)
        urlBuilder.addQueryParameter("code", code)

        disposable = OkHttpHelper.get(urlBuilder.build().toString(), "Accept", "application/json")
                .subscribe({
                    finishActivity(it)
                }, {
                    finishActivity()
                })
    }

    private fun finishActivity() {
        setResult(2)
        finish()
    }

    private fun finishActivity(jsonStr: String) {
        val intent = Intent()
        intent.putExtra(GithubOAuthConstants.RESPONSE_JSON, jsonStr)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }
}