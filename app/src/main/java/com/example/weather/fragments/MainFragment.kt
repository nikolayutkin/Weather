package com.example.weather.fragments

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weather.MainViewModel
import com.example.weather.adapters.VpAdapter
import com.example.weather.adapters.WeatherModel
import com.example.weather.databinding.FragmentMainBinding
import com.example.weather.fragments.DaysFragment
import com.example.weather.fragments.HoursFragment
import com.example.weather.fragments.isPermissionGranted
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import org.json.JSONObject

const val API_KEY = "23e8810f999c40408d7170931230902"

class MainFragment : Fragment() {
    private val fList = listOf(
        HoursFragment.newInstance(),
        DaysFragment.newInstance()
    )
    private val tList = listOf(
        "Hours",
        "Days"
    )
    private lateinit var pLauncher: ActivityResultLauncher<String>
    private lateinit var binding: FragmentMainBinding
    private val model : MainViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        init()
        UpdateCurrentCard()
        requestWeatherData("Yerevan")
    }

    private fun init() = with(binding){
        val adapter = VpAdapter(activity as FragmentActivity, fList)
        vp.adapter = adapter
        TabLayoutMediator(tabLayout, vp){
                tab, pos -> tab.text = tList[pos]
        }.attach()

    }

    private fun UpdateCurrentCard() = with(binding){
        model.liveDataCurrent.observe(viewLifecycleOwner){
            val celsius = "C°"
            val maxMinTemp = "${it.maxTemp}$celsius/${it.minTemp}$celsius"

            tvData.text = it.time
            tvCity.text = it.city
            tvCurrentTemp.text = "${it.currentTemp}$celsius"
            tvCondition.text = it.condition
            tvMaxMin.text = maxMinTemp
            Picasso.get().load("https:" + it.imageUrl).into(imWeatherIcon)
        }
    }

    private fun permissionListener(){
        pLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()){
            Toast.makeText(activity, "Permission is $it", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermission(){
        if(!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)){
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestWeatherData(city: String){
        val url = "https://api.weatherapi.com/v1/forecast.json?key=" +
                API_KEY +
                "&q=" +
                city +
                "&days=" +
                "3" +
                "&aqi=no&alerts=no"
        val queue = Volley.newRequestQueue(context)
        val request = StringRequest(
            Request.Method.GET,
            url,
            {
                    result -> parseWeatherData(result)
            },
            {
                    error -> Log.d("MyLog", "Error: $error")
            }
        )
        queue.add(request)
    }

    private fun parseWeatherData(result: String) {
        val mainObject = JSONObject(result)
        val list = parseDays(mainObject)
        parseCurrentData(mainObject, list[0])
    }

    private fun parseDays(mainObject: JSONObject): List<WeatherModel>{
        val list = ArrayList<WeatherModel>()
        val daysArray = mainObject.getJSONObject("forecast")
            .getJSONArray("forecastday")
        val name =  mainObject.getJSONObject("location").getString("name")
        for (i in 0 until daysArray.length()){
            val day = daysArray[i] as JSONObject
            val item = WeatherModel(
                name,
                day.getString("date"),
                day.getJSONObject("day").getJSONObject("condition")
                    .getString("text"),
                "",
                day.getJSONObject("day").getString("maxtemp_c"),
                day.getJSONObject("day").getString("mintemp_c"),
                day.getJSONObject("day").getJSONObject("condition")
                    .getString("icon"),
                day.getJSONArray("hour").toString()
            )
            list.add(item)
        }
        return list
    }

    private fun parseCurrentData(mainObject: JSONObject, weatherItem: WeatherModel){
        val item = WeatherModel(
            mainObject.getJSONObject("location").getString("name"),
            mainObject.getJSONObject("current").getString("last_updated"),
            mainObject.getJSONObject("current")
                .getJSONObject("condition").getString("text"),
            mainObject.getJSONObject("current").getString("temp_c"),
            weatherItem.maxTemp,
            weatherItem.minTemp,
            mainObject.getJSONObject("current")
                .getJSONObject("condition").getString("icon"),
            weatherItem.hours
        )
        model.liveDataCurrent.value = item
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}
//class MainFragment : Fragment() {
//    private var mFusedLocationClient: FusedLocationProviderClient? = null
//    private val model : MainViewModel by activityViewModels()
//    private val fList = listOf<Fragment>(
//        HoursFragment.newInstance(),
//        DaysFragment.newInstance()
//    )
//    private val tList = listOf(
//        "Hours",
//        "Days"
//    )
//    private var isPermissionGranted = false
//    private lateinit var pLauncher: ActivityResultLauncher<String>
//    private lateinit var binding: FragmentMainBinding
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        binding = FragmentMainBinding.inflate(layoutInflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        binding.ibRefresh.setOnClickListener {
//            binding.tvCurrentTemp.text = ""
//            getLocation()
//        }
//        model.liveDataCurrent.observe(viewLifecycleOwner){
//            binding.apply {
//                val cTemp = try{
//                    "${it.currentTemp.toFloat().toInt()}º"
//                } catch (i: NumberFormatException){
//                    ""
//                }
//                val temp = "${it.maxTemp.toFloat().toInt()}º/${it.minTemp.toFloat().toInt()}º"
//                tvCurrentTemp.text = if(it.currentTemp.isEmpty()) "${it.maxTemp.toFloat().toInt()}º/${it.minTemp.toFloat().toInt()}º" else cTemp
//                tvMaxMinTemp.text = if(it.currentTemp.isNotEmpty()) temp else ""
//                tvWeatherType.text = it.sunType
//                tvCity.text = it.city
//                tvDate.text = if(it.currentTemp.isEmpty())
//                    TimeUtils.changePattern(
//                        "yyyy-MM-dd",
//                        "dd MMM yyyy",
//                        it.time
//                    )
//                else TimeUtils.changePattern(
//                    "yyyy-MM-dd hh:mm",
//                    "dd MMM yyyy / HH:mm",
//                    it.time)
//                Picasso.get().load("https:" + it.imageUri).into(imWeather)
//            }
//        }
//        permissionListener()
//        checkLocationPermission()
//        initVp()
//        checkLocationEnabled()
//    }
//
//    private fun checkLocationEnabled(){
//        val m = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        val isEnabled = m.isProviderEnabled(LocationManager.GPS_PROVIDER)
//        if (!isEnabled){
//           DialogManager.showLocationEnableDialog(activity as AppCompatActivity,
//               object : DialogManager.Listener{
//               override fun onClickYes() {
//                   startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
//               }
//            })
//        }
//    }
//
//    private fun initVp() = with(binding){
//        val adapter = VpAdapter(activity as AppCompatActivity, fList)
//        vp.adapter = adapter
//        TabLayoutMediator(tabLayout, vp){
//            tab, pos-> tab.text = tList[pos]
//        }.attach()
//    }
//
//    private fun checkLocationPermission(){
//        if(checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)){
//            isPermissionGranted = true
//            initLocation()
//            getLocation()
//        } else {
//            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
//        }
//    }
//
//    private fun permissionListener(){
//        pLauncher = registerForActivityResult(
//            ActivityResultContracts.RequestPermission()
//        ){
//            isPermissionGranted = it
//            if(isPermissionGranted){
//                initLocation()
//                getLocation()
//            }
//        }
//    }
//
//    private fun getResult(name: String){
//        val url = "https://api.weatherapi.com/v1/forecast.json" +
//                "?key=$API_KEY&q=$name&days=10&aqi=no&alerts=no"
//        val queue = Volley.newRequestQueue(context)
//        val stringRequest = StringRequest(
//            Request.Method.GET,
//            url,
//            {
//              response->
//                binding.pbRefresh.visibility = View.GONE
//                getDaysList(response)
//            },
//            {
//                Log.d("MyLog","Volley error: $it")
//            }
//        )
//        queue.add(stringRequest)
//    }
//
//    private fun getDaysList(data: String){
//        val gObject = JSONObject(data)
//        val forecastObject = gObject.getJSONObject("forecast")
//        val daysArray = forecastObject.getJSONArray("forecastday")
//        val list = ArrayList<WeatherItem>()
//        for(i in 0 until daysArray.length()){
//            val day = daysArray[i] as JSONObject
//            val item = WeatherItem(
//                gObject.getJSONObject("location").getString("name"),
//                day.getString("date"),
//                day.getJSONObject("day")
//                    .getJSONObject("condition")
//                    .getString("text"),
//                "",
//                day.getJSONObject("day").getString("maxtemp_c"),
//                day.getJSONObject("day").getString("mintemp_c"),
//                day.getJSONObject("day")
//                    .getJSONObject("condition")
//                    .getString("icon"),
//                day.getJSONArray("hour").toString()
//            )
//            list.add(item)
//        }
//        getCurrentWeather(gObject, list, 0)
//        model.liveDayList.value = list
//    }
//
//    private fun getCurrentWeather(jObject: JSONObject, list: List<WeatherItem>, index: Int){
//        val item = WeatherItem(
//            jObject.getJSONObject("location").getString("name"),
//            jObject.getJSONObject("current").getString("last_updated"),
//            jObject.getJSONObject("current")
//                .getJSONObject("condition")
//                .getString("text"),
//            jObject.getJSONObject("current").getString("temp_c"),
//            list[index].maxTemp,
//            list[index].minTemp,
//            jObject.getJSONObject("current")
//                .getJSONObject("condition")
//                .getString("icon"),
//            list[index].hours
//        )
//        model.liveDataCurrent.value = item
//    }
//
//    private fun initLocation(){
//        mFusedLocationClient =
//            LocationServices.getFusedLocationProviderClient(activity as AppCompatActivity)
//    }
//
//    private fun getLocation(){
//        if (
//            !checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)
//            && !checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION
//            )) {
//            return
//        }
//        binding.pbRefresh.visibility = View.VISIBLE
//        val cts = CancellationTokenSource()
//        val task = cts.token
//        val taskL = mFusedLocationClient
//            ?.getCurrentLocation(
//                LocationRequest.PRIORITY_HIGH_ACCURACY,
//                task
//            ) as Task<Location>
//         taskL.addOnCompleteListener {
//             if(it.result != null){
//             val latLon = "${it.result.latitude},${it.result.longitude}"
//             getResult(latLon)
//             } else {
//                 Toast.makeText(activity, "Location not found", Toast.LENGTH_LONG).show()
//             }
//         }
//
//    }
//
//    override fun onDetach() {
//        super.onDetach()
//    }
//
//    companion object {
//        @JvmStatic
//        fun newInstance() = MainFragment()
//    }
//}