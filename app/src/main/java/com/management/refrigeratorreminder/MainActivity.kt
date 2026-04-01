package com.management.refrigeratorreminder

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.management.refrigeratorreminder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val onPrimaryColor by lazy { ContextCompat.getColor(this, R.color.brand_on_primary) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        window.statusBarColor = ContextCompat.getColor(this, R.color.brand_primary)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.brand_surface)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isTopLevel = destination.id in topLevelDestinations
            binding.bottomNavigation.isVisible = isTopLevel
            supportActionBar?.setDisplayHomeAsUpEnabled(!isTopLevel)
            supportActionBar?.title = destination.label
            updateToolbarChrome()
            invalidateOptionsMenu()
        }

        if (intent?.getBooleanExtra(EXTRA_OPEN_HOME, false) == true &&
            navController.currentDestination?.id != R.id.homeFragment
        ) {
            navController.navigate(R.id.homeFragment)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_settings)?.let { item ->
            item.isVisible = navController.currentDestination?.id in topLevelDestinations
            item.icon?.mutate()?.setTint(onPrimaryColor)
        }
        updateToolbarChrome()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> navController.navigateUp()
            R.id.menu_settings -> {
                navController.navigate(R.id.settingsFragment)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateToolbarChrome() {
        binding.toolbar.navigationIcon?.mutate()?.setTint(onPrimaryColor)
        binding.toolbar.overflowIcon?.mutate()?.setTint(onPrimaryColor)
    }

    companion object {
        const val EXTRA_OPEN_HOME = "extra_open_home"

        private val topLevelDestinations = setOf(
            R.id.homeFragment,
            R.id.listFragment,
            R.id.addItemFragment,
        )
    }
}
