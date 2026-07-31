package com.nexsoft.meetingassistant.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.nexsoft.meetingassistant.R
import com.nexsoft.meetingassistant.api.ApiClient
import com.nexsoft.meetingassistant.databinding.ActivityDashboardBinding
import com.nexsoft.meetingassistant.utils.Constants
import com.nexsoft.meetingassistant.utils.SessionManager

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Set token for API calls
        ApiClient.setToken(sessionManager.getToken())

        // Setup navigation menu based on role
        setupNavigationMenu()

        // Setup nav header info
        setupNavHeader()

        // Set dark scrim overlay color when drawer is open
        binding.drawerLayout.setScrimColor(Color.parseColor("#80000000"))

        // Setup menu button to open drawer
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Setup logout button
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        // Setup navigation item selection
        setupNavigationItemSelected()

        // Set default view
        if (savedInstanceState == null) {
            binding.tvTitle.text = "Dashboard"
            binding.tvSubtitle.text = "Halaman Depan"
            loadDefaultWelcomeView()
        }
    }

    private fun setupNavigationMenu() {
        val role = sessionManager.getRole()
        binding.navView.menu.clear()

        when (role) {
            Constants.ROLE_ADMIN -> {
                binding.navView.inflateMenu(R.menu.nav_menu_admin)
            }
            Constants.ROLE_PEMIMPIN_RAPAT -> {
                binding.navView.inflateMenu(R.menu.nav_menu_pemimpin_rapat)
            }
            Constants.ROLE_NOTULIS -> {
                binding.navView.inflateMenu(R.menu.nav_menu_notulis)
            }
        }
    }

    private fun setupNavHeader() {
        val headerView = binding.navView.getHeaderView(0)
        if (headerView != null) {
            val tvHeaderName = headerView.findViewById<TextView>(R.id.tvHeaderName)
            val tvHeaderRole = headerView.findViewById<TextView>(R.id.tvHeaderRole)
            tvHeaderName?.text = sessionManager.getUserName() ?: "User"
            tvHeaderRole?.text = sessionManager.getRole() ?: "Unknown"

            // Setup logo click to go to dashboard
            val logoContainer = headerView.findViewById<View>(R.id.logoContainer)
            logoContainer?.setOnClickListener {
                loadDefaultWelcomeView()
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                
                // Uncheck all menu items
                for (i in 0 until binding.navView.menu.size()) {
                    binding.navView.menu.getItem(i).isChecked = false
                }
            }
        }
    }

    private fun setupNavigationItemSelected() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            var fragment: Fragment? = null
            var title = ""
            var subtitle = ""

            when (menuItem.itemId) {
                R.id.nav_admin -> {
                    fragment = AdminFragment()
                    title = "Admin"
                    subtitle = "Kelola Data Admin"
                }
                R.id.nav_pemimpin_rapat -> {
                    fragment = PemimpinRapatFragment()
                    title = "Pemimpin Rapat"
                    subtitle = "Kelola Data Pemimpin Rapat"
                }
                R.id.nav_notulis -> {
                    fragment = NotulisFragment()
                    title = "Notulis"
                    subtitle = "Kelola Data Notulis"
                }
                R.id.nav_rekaman_rapat -> {
                    fragment = RekamanRapatFragment()
                    title = "Rekaman Rapat"
                    subtitle = "Kelola Data Rekaman Rapat"
                }
                R.id.nav_hasil_transkripsi -> {
                    fragment = HasilTranskripsiListFragment()
                    title = "Hasil Transkripsi"
                    subtitle = "Kelola Data Hasil Transkripsi"
                }
                R.id.nav_laporan -> {
                    fragment = LaporanFragment()
                    title = "Laporan Hasil Transkripsi"
                    subtitle = "Kelola Data Laporan"
                }
            }

            if (fragment != null) {
                loadFragment(fragment)
                binding.tvTitle.text = title
                binding.tvSubtitle.text = subtitle
                menuItem.isChecked = true
            }

            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun loadDefaultWelcomeView() {
        binding.tvTitle.text = "Dashboard"
        binding.tvSubtitle.text = "Halaman Depan"
        val fragment = HomeFragment()
        loadFragment(fragment)
    }

    private fun showLogoutDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_logout, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnLogout = dialogView.findViewById<Button>(R.id.btnLogout)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        btnLogout.setOnClickListener {
            dialog.dismiss()
            performLogout()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun performLogout() {
        sessionManager.clearSession()
        ApiClient.setToken(null)

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
