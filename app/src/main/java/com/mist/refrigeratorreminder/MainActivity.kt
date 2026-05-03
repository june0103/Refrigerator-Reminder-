package com.mist.refrigeratorreminder

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.mist.refrigeratorreminder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val onPrimaryColor by lazy { ContextCompat.getColor(this, R.color.brand_on_primary) }
    private var lastWindowInsets: WindowInsetsCompat? = null
    private val consentManager by lazy {
        (application as RefrigeratorReminderApp).appContainer.privacyConsentManager
    }
    private var bannerAdView: AdView? = null
    private var adLoaded = false
    private var adEligibleDestination = false
    private lateinit var toolbarPadding: InitialPadding
    private lateinit var navHostPadding: InitialPadding
    private lateinit var bottomNavPadding: InitialPadding
    private lateinit var bannerPadding: InitialPadding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setSupportActionBar(binding.toolbar)

        toolbarPadding = binding.toolbar.recordInitialPadding()
        navHostPadding = binding.navHostFragment.recordInitialPadding()
        bottomNavPadding = binding.bottomNavigation.recordInitialPadding()
        bannerPadding = binding.adBannerContainer.recordInitialPadding()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            lastWindowInsets = windowInsets
            applySystemBarInsets(windowInsets)
            windowInsets
        }

        window.statusBarColor = ContextCompat.getColor(this, R.color.brand_primary)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.brand_surface)
        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = true
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isTopLevel = destination.id in topLevelDestinations
            binding.bottomNavigation.isVisible = isTopLevel
            supportActionBar?.setDisplayHomeAsUpEnabled(!isTopLevel)
            supportActionBar?.title = destination.label
            adEligibleDestination = destination.id in bannerDestinations
            updateBannerVisibility()
            if (adEligibleDestination && consentManager.canRequestAds) {
                binding.adBannerContainer.post { loadBannerIfNeeded() }
            }
            updateToolbarChrome()
            lastWindowInsets?.let(::applySystemBarInsets)
            invalidateOptionsMenu()
        }

        consentManager.requestConsent(this) {
            updateBannerVisibility()
            if (adEligibleDestination && consentManager.canRequestAds) {
                binding.adBannerContainer.post { loadBannerIfNeeded() }
            }
        }

        if (intent?.getBooleanExtra(EXTRA_OPEN_HOME, false) == true &&
            navController.currentDestination?.id != R.id.homeFragment
        ) {
            navController.navigate(R.id.homeFragment)
        }

        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun onResume() {
        super.onResume()
        bannerAdView?.resume()
    }

    override fun onPause() {
        bannerAdView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        bannerAdView?.destroy()
        bannerAdView = null
        super.onDestroy()
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

    private fun applySystemBarInsets(windowInsets: WindowInsetsCompat) {
        val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        val displayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())

        val leftInset = maxOf(systemBars.left, displayCutout.left)
        val topInset = maxOf(systemBars.top, displayCutout.top)
        val rightInset = maxOf(systemBars.right, displayCutout.right)
        val bottomInset = maxOf(systemBars.bottom, displayCutout.bottom)

        binding.toolbar.applyInitialPadding(
            initial = toolbarPadding,
            left = toolbarPadding.left + leftInset,
            top = toolbarPadding.top + topInset,
            right = toolbarPadding.right + rightInset,
            bottom = toolbarPadding.bottom,
        )

        binding.bottomNavigation.applyInitialPadding(
            initial = bottomNavPadding,
            left = bottomNavPadding.left + leftInset,
            top = bottomNavPadding.top,
            right = bottomNavPadding.right + rightInset,
            bottom = bottomNavPadding.bottom + bottomInset,
        )

        binding.adBannerContainer.applyInitialPadding(
            initial = bannerPadding,
            left = bannerPadding.left + leftInset,
            top = bannerPadding.top,
            right = bannerPadding.right + rightInset,
            bottom = bannerPadding.bottom,
        )

        val contentBottomInset = if (binding.bottomNavigation.isVisible) 0 else bottomInset
        binding.navHostFragment.applyInitialPadding(
            initial = navHostPadding,
            left = navHostPadding.left + leftInset,
            top = navHostPadding.top,
            right = navHostPadding.right + rightInset,
            bottom = navHostPadding.bottom + contentBottomInset,
        )
    }

    private fun loadBannerIfNeeded() {
        if (!adEligibleDestination) return
        if (!consentManager.canRequestAds) {
            Log.d(TAG, "Banner load deferred. Consent not granted yet.")
            return
        }
        val container = binding.adBannerContainer
        val containerWidthPx = maxOf(container.width, binding.root.width, resources.displayMetrics.widthPixels)
        if (containerWidthPx <= 0) {
            Log.w(TAG, "Banner width is still zero. load skipped.")
            return
        }

        val existing = bannerAdView
        if (existing != null) {
            if (adLoaded) {
                container.isVisible = true
            }
            return
        }

        val adWidth = (containerWidthPx / resources.displayMetrics.density).toInt().coerceAtLeast(320)
        Log.d(
            TAG,
            "Loading banner. env=${getString(R.string.admob_environment_name)}, " +
                "widthPx=$containerWidthPx, widthDp=$adWidth, destinationEligible=$adEligibleDestination",
        )
        val adView = AdView(this).apply {
            adUnitId = getString(R.string.admob_banner_unit_id)
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this@MainActivity, adWidth))
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    adLoaded = true
                    Log.d(
                        TAG,
                        "Banner ad loaded. env=${getString(R.string.admob_environment_name)}, " +
                            "unitSuffix=${adUnitId.takeLast(8)}",
                    )
                    updateBannerVisibility()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adLoaded = false
                    Log.w(
                        TAG,
                        "Banner ad failed to load. " +
                            "code=${loadAdError.code}, domain=${loadAdError.domain}, message=${loadAdError.message}",
                    )
                    binding.adBannerContainer.isVisible = false
                }
            }
        }

        bannerAdView = adView
        container.removeAllViews()
        container.addView(
            adView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        adView.loadAd(AdRequest.Builder().build())
    }

    private fun updateBannerVisibility() {
        binding.adBannerContainer.isVisible = adEligibleDestination && adLoaded && consentManager.canRequestAds
    }

    companion object {
        const val EXTRA_OPEN_HOME = "extra_open_home"
        private const val TAG = "MainActivityAdMob"

        private val topLevelDestinations = setOf(
            R.id.homeFragment,
            R.id.listFragment,
            R.id.addItemFragment,
        )

        private val bannerDestinations = setOf(
            R.id.homeFragment,
            R.id.listFragment,
        )
    }

    private data class InitialPadding(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    )

    private fun View.recordInitialPadding(): InitialPadding {
        return InitialPadding(
            left = paddingLeft,
            top = paddingTop,
            right = paddingRight,
            bottom = paddingBottom,
        )
    }

    private fun View.applyInitialPadding(
        initial: InitialPadding,
        left: Int = initial.left,
        top: Int = initial.top,
        right: Int = initial.right,
        bottom: Int = initial.bottom,
    ) {
        updatePadding(left = left, top = top, right = right, bottom = bottom)
    }
}
