package com.androdevcafe.simbadchecker

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androdevcafe.simbadchecker.model.Application
import com.androdevcafe.simbadchecker.view.components.RecyclerViewApplication
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity() {

    enum class State {
        LOADING, LOADED_INFECTED, LOADED_SAFE
    }

    // Create a callback as member, this way we get a strong reference and the callback can be use in the AsyncTask
    private val getInfectedAppsCallback = { applications: List<Application> ->
        // Build the list
        appList.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        // Create adapter and define callback
        val recyclerViewApplication = RecyclerViewApplication(applications) { application ->
            openSettingsForApplication(application)
        }
        appList.adapter = recyclerViewApplication

        if (applications.isNotEmpty()) {
            infectedApps.text =
                resources.getQuantityString(R.plurals.number_infected_apps, applications.size, applications.size)
            changeState(State.LOADED_INFECTED)
        } else {
            changeState(State.LOADED_SAFE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setTitleTextColor(Color.WHITE)
        setSupportActionBar(toolbar)

        changeState(State.LOADING)

        GetInfectedAppsTask(getInfectedAppsCallback, packageManager, resources.assets)
            .execute()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_refresh -> {
            changeState(State.LOADING)

            GetInfectedAppsTask(getInfectedAppsCallback, packageManager, resources.assets)
                .execute()
            true
        }
        R.id.action_info -> {
            startActivity(Intent(this, InfoActivity::class.java))
            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    private fun openSettingsForApplication(application: Application) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${application.packageName}")
        startActivity(intent)
    }

    private fun changeState(state: State) {
        when (state) {
            State.LOADING -> {
                progressBar.visibility = View.VISIBLE
                checkingTextView.visibility = View.VISIBLE
                appList.visibility = View.GONE
                warning.visibility = View.GONE
                infectedApps.visibility = View.GONE
                ok.visibility = View.GONE
                noInfectedApps.visibility = View.GONE
            }
            State.LOADED_INFECTED -> {
                progressBar.visibility = View.GONE
                checkingTextView.visibility = View.GONE
                appList.visibility = View.VISIBLE
                warning.visibility = View.VISIBLE
                infectedApps.visibility = View.VISIBLE
                ok.visibility = View.GONE
                noInfectedApps.visibility = View.GONE
            }
            State.LOADED_SAFE -> {
                progressBar.visibility = View.GONE
                checkingTextView.visibility = View.GONE
                appList.visibility = View.GONE
                warning.visibility = View.GONE
                infectedApps.visibility = View.GONE
                ok.visibility = View.VISIBLE
                noInfectedApps.visibility = View.VISIBLE
            }
        }
    }

    private class GetInfectedAppsTask(
        onFinish: (List<Application>) -> Unit, val packageManager: PackageManager, val assetManager: AssetManager
    ) : AsyncTask<Unit, Unit, List<Application>>() {

        private val callbackReference: WeakReference<(List<Application>) -> Unit> = WeakReference(onFinish)

        override fun doInBackground(vararg params: Unit?): List<Application> {
            val installedApps = this.packageManager.getInstalledApplications(0)

            val infectedPackagesNames =
                BufferedReader(InputStreamReader(assetManager.open("infected_packages_names.txt"))).readLines()

            return installedApps
                // Remove system apps
                .filter {
                    (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                }
                // Remove non infected app
                .filter {
                    infectedPackagesNames.contains(it.packageName)
                }
                // Transform for interface
                .map {
                    val name = packageManager.getApplicationLabel(it).toString()
                    val icon = packageManager.getApplicationIcon(it)
                    Application(name, it.packageName, icon)
                }
        }

        override fun onPostExecute(result: List<Application>) {
            // Callback to interface if not destroyed
            callbackReference.get()?.invoke(result)
        }
    }
}
