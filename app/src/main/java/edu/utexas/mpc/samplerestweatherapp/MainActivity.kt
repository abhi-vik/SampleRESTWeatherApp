package edu.utexas.mpc.samplerestweatherapp

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage


class MainActivity : AppCompatActivity() {
    lateinit var syncButton: Button
    lateinit var retrieveButton: Button
    lateinit var textView: TextView
    lateinit var imageView: ImageView
    lateinit var publishButton: Button
    lateinit var syncText: TextView

    lateinit var mqttAndroidClient: MqttAndroidClient

    lateinit var queue: RequestQueue
    lateinit var gson: Gson
    lateinit var mostRecentWeatherResult: WeatherResult

    val serverUri = "tcp://192.168.4.1:1883"
    val clientId = "EmergingTechMQTTClient"
    val publishTopic = "weather"
    val subscribeTopic = "steps"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        syncButton = this.findViewById(R.id.syncButton)
        retrieveButton = this.findViewById(R.id.retrieveButton)
        textView = this.findViewById(R.id.text)
        imageView = this.findViewById(R.id.imageView)
        publishButton = this.findViewById(R.id.publishButton)
        syncText = this.findViewById(R.id.syncText)

        queue = Volley.newRequestQueue(this)
        gson = Gson()

        syncButton.setOnClickListener({ syncWithPi() })
        retrieveButton.setOnClickListener({ requestWeather() })
        publishButton.setOnClickListener({ sendWeather() })
        mqttAndroidClient = MqttAndroidClient(getApplicationContext(), serverUri, clientId)
        mqttAndroidClient.setCallback(object: MqttCallbackExtended {

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                println("Connection Complete!!")
                mqttAndroidClient.subscribe(subscribeTopic, 0)
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                println("test")
                println(message)
                syncText.text = message.toString()
            }

            override fun connectionLost(cause: Throwable?) {
                println("Connection Lost")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                println("Delivery Complete")
            }
        })
    }

    fun sendWeather () {
        val weather = mostRecentWeatherResult.weather.get(0).main
        println(weather)
        val message = MqttMessage()
        message.payload = weather.toByteArray()
        println(message)
        println(mqttAndroidClient)
        mqttAndroidClient.publish(publishTopic, message)
    }

    fun requestWeather() {
        val url = StringBuilder("https://api.openweathermap.org/data/2.5/weather?id=4254010&appid=03519fb228fd7abd9e4f94d06d81eb27").toString()
        val stringRequest = object : StringRequest(com.android.volley.Request.Method.GET, url,
                com.android.volley.Response.Listener<String> { response ->
                    //textView.text = response
                    mostRecentWeatherResult = gson.fromJson(response, WeatherResult::class.java)
                    val weather = mostRecentWeatherResult.weather.get(0)
                    val icon = weather.icon
                    textView.text = weather.main
                    println(imageView)
                    Picasso.get().load("https://openweathermap.org/img/wn/$icon@2x.png").into(imageView)
                },
                com.android.volley.Response.ErrorListener { println("******That didn't work!") }) {}
        queue.add(stringRequest)
    }

    fun syncWithPi(){
        println("+++++++ Connecting...")
        mqttAndroidClient.connect()
    }

}

class WeatherResult(val id: Int, val name: String, val cod: Int, val coord: Coordinates, val main: WeatherMain, val weather: Array<Weather>)
class Coordinates(val lon: Double, val lat: Double)
class Weather(val id: Int, val main: String, val description: String, val icon: String)
class WeatherMain(val temp: Double, val pressure: Int, val humidity: Int, val temp_min: Double, val temp_max: Double)

