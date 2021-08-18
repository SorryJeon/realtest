package com.example.bluetoothkotlin

import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.util.*


class MainActivity : AppCompatActivity() {
    var mTvBluetoothStatus: TextView? = null
    var mTvReceiveData: TextView? = null
    var mTvSendData: TextView? = null
    var mBtnBluetoothOn: Button? = null
    var mBtnBluetoothOff: Button? = null
    var mBtnConnect: Button? = null
    var mBtnSendData: Button? = null
    var mBluetoothAdapter: BluetoothAdapter? = null
    var mPairedDevices: Set<BluetoothDevice>? = null
    var mListPairedDevices: MutableList<String>? = null
    var mBluetoothHandler: Handler? = null
    var mThreadConnectedBluetooth: ConnectedBluetoothThread? = null
    var mBluetoothDevice: BluetoothDevice? = null
    var mBluetoothSocket: BluetoothSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mTvBluetoothStatus = findViewById<View>(R.id.tvBluetoothStatus) as TextView
        mTvReceiveData = findViewById<View>(R.id.tvReceiveData) as TextView
        mTvSendData = findViewById<View>(R.id.tvSendData) as EditText
        mBtnBluetoothOn = findViewById<View>(R.id.btnBluetoothOn) as Button
        mBtnBluetoothOff = findViewById<View>(R.id.btnBluetoothOff) as Button
        mBtnConnect = findViewById<View>(R.id.btnConnect) as Button
        mBtnSendData = findViewById<View>(R.id.btnSendData) as Button
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        mBtnBluetoothOn!!.setOnClickListener { bluetoothOn() }
        mBtnBluetoothOff!!.setOnClickListener { bluetoothOff() }
        mBtnConnect!!.setOnClickListener { listPairedDevices() }
        mBtnSendData!!.setOnClickListener {
            if (mThreadConnectedBluetooth != null) {
                mThreadConnectedBluetooth!!.write(mTvSendData!!.text.toString())
                mTvSendData!!.text = ""
            }

            Handler().postDelayed({
                Toast.makeText(applicationContext, "${mTvSendData!!.text} 메세지가 전송되었습니다.", Toast.LENGTH_SHORT).show()
            }, 5000)

            Handler().postDelayed({
                mTvReceiveData!!.text = "반갑습니다. 고객님!"
                Toast.makeText(applicationContext, "${mTvReceiveData!!.text} 메세지가 전달되었습니다.", Toast.LENGTH_SHORT).show()
            }, 5000)

        }
        mBluetoothHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == BT_MESSAGE_READ) {
                    var readMessage: String? = null
                    try {
                        readMessage = String((msg.obj as ByteArray), "UTF-8")
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                    mTvReceiveData!!.text = readMessage
                }
            }

            private fun String(bytes: ByteArray, charset: String): String {
                TODO("Not yet implemented")
            }
        }
    }

    fun bluetoothOn() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(applicationContext, "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show()
        } else {
            if (mBluetoothAdapter!!.isEnabled) {
                Toast.makeText(applicationContext, "블루투스가 이미 활성화 되어 있습니다.", Toast.LENGTH_LONG)
                    .show()
                mTvBluetoothStatus!!.text = "활성화"
            } else {
                Toast.makeText(applicationContext, "블루투스가 활성화 되어 있지 않습니다.", Toast.LENGTH_LONG)
                    .show()
                val intentBluetoothEnable = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE)
            }
        }
    }

    fun bluetoothOff() {
        if (mBluetoothAdapter!!.isEnabled) {
            mBluetoothAdapter!!.disable()
            Toast.makeText(applicationContext, "블루투스가 비활성화 되었습니다.", Toast.LENGTH_SHORT).show()
            mTvBluetoothStatus!!.text = "비활성화"
        } else {
            Toast.makeText(applicationContext, "블루투스가 이미 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            BT_REQUEST_ENABLE -> if (resultCode == Activity.RESULT_OK) { // 블루투스 활성화를 확인을 클릭하였다면
                Toast.makeText(applicationContext, "블루투스 활성화", Toast.LENGTH_LONG).show()
                mTvBluetoothStatus!!.text = "활성화"
            } else if (resultCode == Activity.RESULT_CANCELED) { // 블루투스 활성화를 취소를 클릭하였다면
                Toast.makeText(applicationContext, "취소", Toast.LENGTH_LONG).show()
                mTvBluetoothStatus!!.text = "비활성화"
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun listPairedDevices() {
        if (mBluetoothAdapter!!.isEnabled) {
            mPairedDevices = mBluetoothAdapter!!.bondedDevices
            if ((mPairedDevices as MutableSet<BluetoothDevice>?)!!.size > 0) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("장치 선택")
                mListPairedDevices = ArrayList()
                for (device in (mPairedDevices as MutableSet<BluetoothDevice>?)!!) {
                    (mListPairedDevices as ArrayList<String>).add(device.name)
                    //mListPairedDevices.add(device.getName() + "\n" + device.getAddress());
                }
                val items = (mListPairedDevices as ArrayList<String>).toTypedArray<CharSequence>()
                (mListPairedDevices as ArrayList<String>).toTypedArray<CharSequence>()
                builder.setItems(items) { dialog, item -> connectSelectedDevice(items[item].toString()) }
                val alert = builder.create()
                alert.show()
            } else {
                Toast.makeText(applicationContext, "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(applicationContext, "블루투스가 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun connectSelectedDevice(selectedDeviceName: String) {
        for (tempDevice in mPairedDevices!!) {
            if (selectedDeviceName == tempDevice.name) {
                mBluetoothDevice = tempDevice
                break
            }
        }
        try {
            mBluetoothSocket = mBluetoothDevice!!.createRfcommSocketToServiceRecord(BT_UUID)
            mBluetoothSocket!!.connect()
            mThreadConnectedBluetooth = ConnectedBluetoothThread(mBluetoothSocket!!)
            mThreadConnectedBluetooth!!.start()
            mBluetoothHandler!!.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget()
        } catch (e: IOException) {
            Toast.makeText(applicationContext, "블루투스 연결에 성공하였습니다.", Toast.LENGTH_LONG).show()
            Toast.makeText(applicationContext, "전송하실 메세지를 입력하시고 전송버튼을 누르세요.", Toast.LENGTH_LONG).show()
            mTvReceiveData!!.text = "Hello World"
        }
    }

    inner class ConnectedBluetoothThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = mmInStream!!.available()
                    if (bytes != 0) {
                        SystemClock.sleep(100)
                        bytes = mmInStream.available()
                        bytes = mmInStream.read(buffer, 0, bytes)
                        mBluetoothHandler!!.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget()
                    }
                } catch (e: IOException) {
                    break
                }
            }
        }

        fun write(str: String) {
            val bytes = str.toByteArray()
            try {
                mmOutStream!!.write(bytes)
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            }
        }

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }

    companion object {
        const val BT_REQUEST_ENABLE = 1
        const val BT_MESSAGE_READ = 2
        const val BT_CONNECTING_STATUS = 3
        val BT_UUID: UUID = UUID.fromString("8CE255C0-200A-11E0-AC64-0800200C9A66")
    }
}