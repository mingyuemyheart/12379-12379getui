package com.warning.activity

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import com.umeng.message.UmengNotifyClickActivity
import com.warning.R
import com.warning.common.CONST
import com.warning.manager.DBManager
import com.warning.util.CommonUtil
import com.warning.util.OkHttpUtil
import kotlinx.android.synthetic.main.activity_warning_detail_hw_mi_mz.*
import kotlinx.android.synthetic.main.shawn_layout_title.*
import net.tsz.afinal.FinalBitmap
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.android.agoo.common.AgooConstants
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * 预警信息详情
 */
class WarningDetailHWMIMZActivity : UmengNotifyClickActivity(), OnClickListener {

    private var dataUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_warning_detail_hw_mi_mz)
        initRefreshLayout()
        initWidget()
    }

    /**
     * 初始化下拉刷新布局
     */
    private fun initRefreshLayout() {
        refreshLayout!!.setColorSchemeResources(CONST.color1, CONST.color2, CONST.color3, CONST.color4)
        refreshLayout!!.setProgressViewEndTarget(true, 300)
        refreshLayout!!.isRefreshing = true
        refreshLayout!!.setOnRefreshListener { refresh() }
    }

    /**
     * 初始化控件
     */
    private fun initWidget() {
        llBack!!.setOnClickListener(this)
        tvTitle!!.text = getString(R.string.warning_detail)
        ivShare!!.setOnClickListener(this)
        ivShare!!.visibility = View.GONE
    }

    private fun refresh() {
        if (!TextUtils.isEmpty(dataUrl)) {
            okHttpWarningDetail(dataUrl)
        } else {
            refreshLayout!!.isRefreshing = false
        }
    }

    override fun onMessage(intent: Intent) {
        super.onMessage(intent)
        val body = intent.getStringExtra(AgooConstants.MESSAGE_BODY)
        Log.e("body", body)
        runOnUiThread {
            if (!TextUtils.isEmpty(body)) {
                try {
                    val obj = JSONObject(body)
                    if (!obj.isNull("extra")) {
                        val extra = obj.getJSONObject("extra")
                        if (!extra.isNull("url")) {
                            val url = extra.getString("url")
                            dataUrl = "https://decision-admin.tianqi.cn/Home/work2019/getDetailWarn/identifier/$url"
                            refresh()
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 获取预警详情
     */
    private fun okHttpWarningDetail(url: String?) {
        Thread {
            OkHttpUtil.enqueue(Request.Builder().url(url!!).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    runOnUiThread {
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                val `object` = JSONObject(result)
                                if (`object` != null) {
                                    if (!`object`.isNull("sendTime")) {
                                        tvTime.text = `object`.getString("sendTime")
                                    }
                                    if (!`object`.isNull("description")) {
                                        tvIntro.text = `object`.getString("description")
                                    }
                                    val name = `object`.getString("headline")
                                    if (!TextUtils.isEmpty(name)) {
                                        tvName.text = name.replace(getString(R.string.publish), getString(R.string.publish)+"\n")
                                    }
                                    var bitmap: Bitmap? = null
                                    val color = `object`.getString("severityCode")
                                    val type = `object`.getString("eventType")
                                    if (color == CONST.blue[0]) {
                                        bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailHWMIMZActivity, "warning/" + type + CONST.blue[1] + CONST.imageSuffix)
                                        if (bitmap == null) {
                                            bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailHWMIMZActivity, "warning/" + "default" + CONST.blue[1] + CONST.imageSuffix)
                                        }
                                    } else if (color == CONST.yellow[0]) {
                                        bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailHWMIMZActivity, "warning/" + type + CONST.yellow[1] + CONST.imageSuffix)
                                        if (bitmap == null) {
                                            bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailHWMIMZActivity, "warning/" + "default" + CONST.yellow[1] + CONST.imageSuffix)
                                        }
                                    } else if (color == CONST.orange[0]) {
                                        bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailHWMIMZActivity, "warning/" + type + CONST.orange[1] + CONST.imageSuffix)
                                        if (bitmap == null) {
                                            bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailHWMIMZActivity, "warning/" + "default" + CONST.orange[1] + CONST.imageSuffix)
                                        }
                                    } else if (color == CONST.red[0]) {
                                        bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailHWMIMZActivity, "warning/" + type + CONST.red[1] + CONST.imageSuffix)
                                        if (bitmap == null) {
                                            bitmap = CommonUtil.getImageFromAssetsFile(this@WarningDetailHWMIMZActivity, "warning/" + "default" + CONST.red[1] + CONST.imageSuffix)
                                        }
                                    }
                                    imageView.setImageBitmap(bitmap)
                                    if (!`object`.isNull("identifier")) {
                                        val identifier = `object`.getString("identifier")
                                        if (!TextUtils.isEmpty(identifier)) {
                                            val imgUrl = String.format("http://12379.tianqi.cn/Public/gw_html_imgs/%s.png", identifier)
                                            val finalBitmap = FinalBitmap.create(this@WarningDetailHWMIMZActivity)
                                            finalBitmap.display(ivPicture, imgUrl, null, 0)
                                        }
                                    }
                                    initDBManager(color, type)
                                    scrollView!!.visibility = View.VISIBLE
                                    ivShare!!.visibility = View.VISIBLE
                                    refreshLayout!!.isRefreshing = false
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            })
        }.start()
    }

    /**
     * 初始化数据库
     */
    private fun initDBManager(color: String, type: String) {
        val dbManager = DBManager(this@WarningDetailHWMIMZActivity)
        dbManager.openDateBase()
        dbManager.closeDatabase()
        val database = SQLiteDatabase.openOrCreateDatabase(DBManager.DB_PATH + "/" + DBManager.DB_NAME, null)
        val cursor = database.rawQuery("select * from " + DBManager.TABLE_NAME2 + " where WarningId = " + "\"" + type + color + "\"", null)
        for (i in 0 until cursor.count) {
            cursor.moveToPosition(i)
            tvGuide.text = getString(R.string.warning_guide).toString() + cursor.getString(cursor.getColumnIndex("WarningGuide"))
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.llBack -> finish()
            R.id.ivShare -> {
                val bitmap1 = CommonUtil.captureScrollView(scrollView)
                val bitmap2 = BitmapFactory.decodeResource(resources, R.drawable.iv_share_bottom)
                val bitmap = CommonUtil.mergeBitmap(this@WarningDetailHWMIMZActivity, bitmap1, bitmap2, false)
                CommonUtil.clearBitmap(bitmap1)
                CommonUtil.clearBitmap(bitmap2)
                CommonUtil.share(this@WarningDetailHWMIMZActivity, bitmap)
            }
        }
    }

}
