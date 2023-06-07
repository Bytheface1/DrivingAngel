package com.example.test.ui.notifications

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test.MainActivity
import com.example.test.data.database.entities.HeartRateEntity
import com.example.test.databinding.FragmentNotificationsBinding
import com.example.test.ui.DrivingAngelApp
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.Legend.LegendForm
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.util.*
import kotlin.math.sqrt


class NotificationsFragment : Fragment(), CoroutineScope by MainScope(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    private var _binding: FragmentNotificationsBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Variables for connection between mobile and smartwatch
    private var activityContext: Context? = null
    private val wearableAppCheckPayload = "AppOpenWearable"
    private val wearableAppCheckPayloadReturnACK = "AppOpenWearableACK"
    private var wearableDeviceConnected: Boolean = false

    private var currentAckFromWearForAppOpenCheck: String? = null
    private val APP_OPEN_WEARABLE_PAYLOAD_PATH = "/APP_OPEN_WEARABLE_PAYLOAD"

    private val MESSAGE_ITEM_RECEIVED_PATH: String = "/message-item-received"

    private val TAG_GET_NODES: String = "getnodes1"
    private val TAG_MESSAGE_RECEIVED: String = "receive1"

    private var messageEvent: MessageEvent? = null
    private var wearableNodeUri: String? = null


    private val notificationsViewModel: NotificationsViewModel by viewModels {
        DrivingAngelViewModelFactory((this@NotificationsFragment.activity?.application as DrivingAngelApp).repository)
    }

    private lateinit var lineChart: LineChart
    private var indexActualized = 0


    // Variables for Sleepiness Calculation
    private var avg = 0.0
    private var sum = 0.0
    private var squareSum = 0.0
    private var quantity: Int = 0
    private var firstDate = true
    private var finalDate = addXMin(Date(), 30)
    private var restDate = addXMin(Date(), 120)
    private var initialMean = 0.0
    private var currentMean = 0.0
    private var initialStandardDeviation = 0.0
    private var currentStandardDeviation = 0.0
    private var initialVariationCoefficient = 0.0
    private var currentVariationCoefficient = 0.0
    private var initialState = true


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        activityContext = this.context
        wearableDeviceConnected = false
        val recyclerView = binding.recyclerview
        val adapter = HeartRateListAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        notificationsViewModel.deleteAll()      // Begin the APP without data
        lineChart = binding.lineGraph

        initializeLineChart()


        binding.checkwearablesButton.setOnClickListener {
            if (!wearableDeviceConnected) {
                val tempAct: Activity = activityContext as MainActivity
                //Coroutine
                initialiseDevicePairing(tempAct)
            }
        }

        // Add an observer on the LiveData returned by getAllHeartRates.
        // The onChanged() method fires when the observed data changes and the activity is
        // in the foreground.
        notificationsViewModel.allHeartRates.observe(viewLifecycleOwner) { heartRates ->
            // Update the cached copy of the heart rates in the adapter.
            heartRates.let {
                adapter.submitList(it)
                recyclerView.smoothScrollToPosition(it.size)
            }
            if (heartRates.isNotEmpty()) {
                addHRToChart(heartRates.last())
                sleepDetection(heartRates)
            }
        }
        return root
    }

    /**
     * Function to create the line chart data set
     */
    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Heart rate")
        set.axisDependency = AxisDependency.LEFT
        //set.setColors(*ColorTemplate.PASTEL_COLORS)
        set.color = ColorTemplate.getHoloBlue()
        set.setCircleColor(Color.WHITE)
        set.lineWidth = 2f
        set.circleRadius = 4f
        set.fillAlpha = 65
        set.fillColor = ColorTemplate.getHoloBlue()
        set.highLightColor = Color.rgb(244, 117, 117)
        set.valueTextColor = Color.BLUE
        set.valueTextSize = 9f
        set.setDrawValues(false)

        return set
    }

    /**
     * Function to initialize the line chart
     */
    private fun initializeLineChart(){
        // enable description text
        lineChart.description.isEnabled = true

        // enable touch gestures
        lineChart.setTouchEnabled(true)

        // enable scaling and dragging
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(false)
        lineChart.setDrawGridBackground(false)


        // if disabled, scaling can be done on x- and y-axis separately
        lineChart.setPinchZoom(true)

        // set an alternative background color
        lineChart.setBackgroundColor(Color.LTGRAY)

        val data = LineData()
        data.setValueTextColor(Color.BLUE)

        // add empty data
        lineChart.data = data
        indexActualized = 0

        // get the legend (only possible after setting data)
        val l: Legend = lineChart.legend

        // modify the legend ...
        l.form = LegendForm.LINE
        l.typeface = Typeface.DEFAULT
        l.textColor = Color.BLUE

        val xl: XAxis = lineChart.xAxis
        xl.typeface = Typeface.DEFAULT
        xl.textColor = Color.BLUE
        xl.setDrawGridLines(false)
        xl.setAvoidFirstLastClipping(true)
        xl.isEnabled = false

        val leftAxis: YAxis = lineChart.axisLeft
        leftAxis.typeface = Typeface.DEFAULT
        leftAxis.textColor = Color.BLUE
        leftAxis.axisMaximum = 120f
        leftAxis.axisMinimum = 40f
        leftAxis.setDrawGridLines(true)

        val rightAxis: YAxis = lineChart.axisRight
        rightAxis.isEnabled = false
    }

    private fun addEntry(itemHeartRateList: HeartRateEntity) {

        val data: LineData = lineChart.data

        var set = data.getDataSetByIndex(0)
        if (set == null) {
            set = createSet()
            data.addDataSet(set)
        }
        data.addEntry(
            Entry(set.entryCount.toFloat(), itemHeartRateList.heart_rate.toFloat()), 0
        )

        data.notifyDataChanged()

        // let the chart know it's data has changed
        lineChart.notifyDataSetChanged()

        // limit the number of visible entries
        lineChart.setVisibleXRangeMaximum(15f)

        // move to the latest entry
        lineChart.moveViewToX(data.entryCount.toFloat())

        //add animation
        /*lineChart.animateX(1000, Easing.EaseInSine)
        lineChart.data = data
        //refresh
        lineChart.invalidate()*/
        // this automatically refreshes the chart (calls invalidate())
        //lineChart.moveViewTo(data.getXValCount()-7, 55f,
        // AxisDependency.LEFT);
    }


    /**
     * Function to add a heart rate entry to chart
     */
    private fun addHRToChart(last: HeartRateEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            runBlocking { addEntry(last) }
            try {
                delay(25)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * This function receives a list of HeartRateEntity and detects sleepiness based on certain
     * characteristics of the heart rates.
     */
    private fun sleepDetection(heartRates: List<HeartRateEntity>) {
        CoroutineScope(Dispatchers.IO).launch {
            runBlocking {
                if (initialState) {     // First period
                    if (Date().before(finalDate)) {
                        addNumber(heartRates.last().heart_rate)
                        currentVariationCoefficient = variationCoefficientCalculation()
                    } else {
                        val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
                        toneGenerator.startTone(
                            ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE,
                            3000
                        )
                        (activityContext as Activity).runOnUiThread {
                            Toast.makeText(
                                activityContext,
                                "initialMean = ${BigDecimal(currentMean).setScale(2, RoundingMode.HALF_EVEN).toDouble()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        initialState = false
                        initialMean = currentMean
                        initialStandardDeviation = currentStandardDeviation
                        initialVariationCoefficient = currentVariationCoefficient
                        avg = 0.0
                        sum = 0.0
                        squareSum = 0.0
                        quantity = 0
                        finalDate = addXMin(Date(), 1)
                    }

                } else {    //compare initial and current states
                    if (Date().before(restDate)){
                        if (Date().before(finalDate)) {
                            addNumber(heartRates.last().heart_rate)
                            currentVariationCoefficient = variationCoefficientCalculation()
                            if (meanDifference(initialMean, currentMean) < -6 &&
                                calculateDecrease(initialVariationCoefficient, currentVariationCoefficient) <= -10.0 &&     // 10% decreasing
                                currentStandardDeviation < 10) {
                                val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
                                toneGenerator.startTone(
                                    ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_PING_RING,
                                    3000
                                )
                                (activityContext as Activity).runOnUiThread {
                                    Toast.makeText(
                                        activityContext,
                                        "curSD = ${BigDecimal(currentStandardDeviation).setScale(2, RoundingMode.HALF_EVEN).toDouble()} " +
                                                "curCV = ${BigDecimal(currentVariationCoefficient).setScale(2, RoundingMode.HALF_EVEN).toDouble()} " +
                                                "curMN = ${BigDecimal(currentMean).setScale(2, RoundingMode.HALF_EVEN).toDouble()}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            avg = 0.0
                            sum = 0.0
                            squareSum = 0.0
                            quantity = 0
                            finalDate = addXMin(Date(), 1)
                        }
                    } else {
                        val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
                        toneGenerator.startTone(
                            ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
                            3000
                        )
                        (activityContext as Activity).runOnUiThread {
                            Toast.makeText(
                                activityContext,
                                "Stop at the nearest service area",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            try {
                delay(25)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Function to add the last heart rate value for sleepiness calculations dynamically
     */
    private fun addNumber(num: Int) {
        quantity++
        sum += num
        val delta = num.toDouble() - avg
        avg += delta / quantity
        squareSum += delta * (num - avg)
    }

    /**
     * Function to calculate the average dynamically
     */
    private fun averageUpdateable(): Double {
        return if (quantity > 0) sum / quantity else 0.0
    }

    /**
     * Function to calculate the Standard Deviation
     */
    private fun standardDeviationUpdateable(): Double {
        return if (quantity > 0) sqrt(squareSum / quantity) else 0.0
    }

    /**
     * Function to calculate the coefficient of variation
     */
    private fun variationCoefficientCalculation(): Double {
        currentMean = averageUpdateable()
        currentStandardDeviation = standardDeviationUpdateable()
        return if (currentMean > 0) currentStandardDeviation / currentMean else 0.0
    }

    /**
     * Function to calculate the percentage difference of two means
     */
    private fun meanDifference(initialMean: Double, currentMean: Double): Double {
        return (currentMean * 100 / initialMean - 100)
    }

    /**
     * Function to calculate decrease percentage of coefficient of variation
     */
    private fun calculateDecrease(initialcv: Double, currentcv: Double): Double {
        return ((currentcv - initialcv) / currentcv) * 100.0
    }

    // CONNECTION PART

    @SuppressLint("SetTextI18n")
    private fun initialiseDevicePairing(tempAct: Activity) {
        //Coroutine
        launch(Dispatchers.Default) {
            var getNodesResBool: BooleanArray? = null

            try {
                getNodesResBool =
                    getNodes(tempAct.applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            //UI Thread
            withContext(Dispatchers.Main) {
                if (getNodesResBool!![0]) {
                    //if message Acknowledgement Received
                    if (getNodesResBool[1]) {
                        Toast.makeText(
                            activityContext,
                            "Wearable device paired and app is open.",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.deviceconnectionStatusTv.text =
                            "Wearable device paired and app is open."
                        binding.deviceconnectionStatusTv.visibility = View.VISIBLE
                        wearableDeviceConnected = true
                    } else {
                        Toast.makeText(
                            activityContext,
                            "A wearable device is paired but the wearable app on your watch isn't open. Launch the wearable app and try again.",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.deviceconnectionStatusTv.text =
                            "Wearable device paired but app isn't open."
                        binding.deviceconnectionStatusTv.visibility = View.VISIBLE
                        wearableDeviceConnected = false
                    }
                } else {
                    Toast.makeText(
                        activityContext,
                        "No wearable device paired. Pair a wearable device to your phone using the Wear OS app and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.deviceconnectionStatusTv.text =
                        "Wearable device not paired and connected."
                    binding.deviceconnectionStatusTv.visibility = View.VISIBLE
                    wearableDeviceConnected = false
                }
            }
        }
    }


    private fun getNodes(context: Context): BooleanArray {
        val nodeResults = HashSet<String>()
        val resBool = BooleanArray(2)
        resBool[0] = false //nodePresent
        resBool[1] = false //wearableReturnAckReceived
        val nodeListTask =
            Wearable.getNodeClient(context).connectedNodes
        try {
            // Block on a task and get the result synchronously (because this is on a background thread).
            val nodes =
                Tasks.await(
                    nodeListTask
                )
            Log.e(TAG_GET_NODES, "Task fetched nodes")
            for (node in nodes) {
                Log.e(TAG_GET_NODES, "inside loop")
                nodeResults.add(node.id)
                try {
                    val nodeId = node.id
                    // Set the data of the message to be the bytes of the Uri.
                    val payload: ByteArray = wearableAppCheckPayload.toByteArray()
                    // Send the rpc
                    // Instantiates clients without member variables, as clients are inexpensive to
                    // create. (They are cached and shared between GoogleApi instances.)
                    val sendMessageTask =
                        Wearable.getMessageClient(context)
                            .sendMessage(nodeId, APP_OPEN_WEARABLE_PAYLOAD_PATH, payload)
                    try {
                        // Block on a task and get the result synchronously (because this is on a background thread).
                        val result = Tasks.await(sendMessageTask)
                        Log.d(TAG_GET_NODES, "send message result : $result")
                        resBool[0] = true

                        //Wait for 700 ms/0.7 sec for the acknowledgement message
                        //Wait 1
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(100)
                            Log.d(TAG_GET_NODES, "ACK thread sleep 1")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        //Wait 2
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(250)
                            Log.d(TAG_GET_NODES, "ACK thread sleep 2")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            resBool[1] = true
                            return resBool
                        }
                        //Wait 3
                        if (currentAckFromWearForAppOpenCheck != wearableAppCheckPayloadReturnACK) {
                            Thread.sleep(350)
                            Log.d(TAG_GET_NODES, "ACK thread sleep 5")
                        }
                        if (currentAckFromWearForAppOpenCheck == wearableAppCheckPayloadReturnACK) {
                            Log.d(TAG_GET_NODES, "ACK thread true")
                            resBool[1] = true
                            return resBool
                        }
                        resBool[1] = false
                        Log.d(
                            TAG_GET_NODES,
                            "ACK thread timeout, no message received from the wearable "
                        )
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                } catch (e1: Exception) {
                    Log.d(TAG_GET_NODES, "send message exception")
                    e1.printStackTrace()
                }
            }
        } catch (exception: Exception) {
            Log.e(TAG_GET_NODES, "Task failed: $exception")
            exception.printStackTrace()
        }
        return resBool
    }


    override fun onDataChanged(p0: DataEventBuffer) {
    }

    @SuppressLint("SetTextI18n")
    override fun onMessageReceived(p0: MessageEvent) {
        try {
                val btMessage =
                    String(p0.data, StandardCharsets.UTF_8)
                val messageEventPath: String = p0.path
                Log.d(
                    TAG_MESSAGE_RECEIVED,
                    "onMessageReceived() Received a message from watch:"
                            + p0.requestId
                            + " "
                            + messageEventPath
                            + " "
                            + btMessage
                )
                if (messageEventPath == APP_OPEN_WEARABLE_PAYLOAD_PATH) {
                    currentAckFromWearForAppOpenCheck = btMessage
                    Log.d(
                        TAG_MESSAGE_RECEIVED,
                        "Received acknowledgement message that app is open in wear"
                    )

                    val sbTemp = StringBuilder()
                    sbTemp.append("\nWearable device connected.")
                    Log.d("receive1", " $sbTemp")

                    binding.checkwearablesButton.visibility = View.GONE
                    messageEvent = p0
                    wearableNodeUri = p0.sourceNodeId
                } else if (messageEventPath.isNotEmpty() && messageEventPath == MESSAGE_ITEM_RECEIVED_PATH) {
                    if (wearableDeviceConnected) {
                        try {
                            if (firstDate) {
                                finalDate = addXMin(Date(), 15)
                                firstDate = false
                                restDate = addXMin(Date(), 120)     // Alert user in 2 hours for drive rest
                            }
                            val format = NumberFormat.getInstance(Locale.getDefault())
                            val valueHR = format.parse(btMessage)?.toDouble()?.toInt() ?: 0
                            if (valueHR >= 35) { // Check if sensor works
                                val heartListItem = HeartRateEntity(0, valueHR, Date())

                                val sbTemp = StringBuilder()
                                sbTemp.append("\n")
                                sbTemp.append(btMessage)
                                sbTemp.append(" - (Received from wearable)")
                                Log.d("receive1", " $sbTemp")
                                notificationsViewModel.insert(heartListItem)
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("receive1", "Handled")
        }
    }

    /**
     * Function to add X minutes from a Date
     */
    private fun addXMin(date: Date, minutes: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.MINUTE, minutes)
        return calendar.time
    }

    override fun onCapabilityChanged(p0: CapabilityInfo) {
    }


    override fun onPause() {
        super.onPause()
        try {
            Wearable.getDataClient(activityContext!!).removeListener(this)
            Wearable.getMessageClient(activityContext!!).removeListener(this)
            Wearable.getCapabilityClient(activityContext!!).removeListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onResume() {
        super.onResume()
        try {
            Wearable.getDataClient(activityContext!!).addListener(this)
            Wearable.getMessageClient(activityContext!!).addListener(this)
            Wearable.getCapabilityClient(activityContext!!)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}