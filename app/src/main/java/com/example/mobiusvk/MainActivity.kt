package com.example.mobiusvk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn.setOnClickListener {
            launchNewActivity(this, "com.signnow.android")
        }
    }

    fun launchNewActivity(context: Context, packageName: String) {
//        var intent: Intent? = context.packageManager.getLaunchIntentForPackage(packageName)
//        var intent = Intent()
//        intent.action = "pdffiller.import.DOCUMENT"
//        intent?.putExtra("JWT", "JwToken")
//        if (intent == null) {
//            try {
//                intent = Intent(Intent.ACTION_VIEW)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                intent.data = Uri.parse("market://details?id=$packageName")
//                context.startActivity(intent)
//            } catch (anfe: android.content.ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.signnow.android&referrer=utm_source%3Dfill%26utm_medium%3Demail%26utm_term%3Dreferer%26utm_content%3Dproject_id%26utm_campaign%3DJWT")
                    )
                )
//            }

//        } else {
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
//            context.startActivity(intent)
//        }
    }


}
