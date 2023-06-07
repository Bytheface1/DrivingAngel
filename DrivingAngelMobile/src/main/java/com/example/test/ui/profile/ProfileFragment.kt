package com.example.test.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.test.R
import com.example.test.dataStore
import com.example.test.databinding.FragmentProfileBinding
import com.example.test.model.UserProfile
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /*val profileViewModel =
            ViewModelProvider(this).get(ProfileViewModel::class.java)*/

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        lifecycleScope.launch(Dispatchers.IO) {
            retrieveProfile().collect {
                withContext(Dispatchers.Main) {
                    if (it.gender) {
                        binding.radioButton1.isChecked = true
                    } else {
                        binding.radioButton2.isChecked = true
                    }
                    binding.switchAthlete.isChecked = it.athlete
                    binding.agePicker.value = it.age

                }
            }
        }
        setupPicker()
        checkGender()

        binding.btnSaveProfile.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) { saveProfile() }
            Snackbar.make(binding.agePicker.rootView, getString(R.string.profile_saved), LENGTH_SHORT)
                .setAnchorView(binding.tvProfile.id).show()
        }
        /*val textView: TextView = binding.tvProfile
        profileViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }*/
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupPicker() {
        val agePicker = binding.agePicker
        agePicker.minValue = 16
        agePicker.maxValue = 99
        agePicker.wrapSelectorWheel = true
        agePicker.setOnValueChangedListener { _, oldVal, newVal ->
            val text = "Changed age from $oldVal to $newVal"
            Snackbar.make(binding.agePicker.rootView, text, LENGTH_SHORT)
                .setAnchorView(binding.tvProfile.id).show()
        }
    }

    private fun checkGender() {
        //val checkedRadioButtonId = binding.radioGroupGender.checkedRadioButtonId // Returns View.NO_ID if nothing is checked.
        binding.radioGroupGender.setOnCheckedChangeListener { _, _ ->
            // Responds to child RadioButton checked/unchecked
            Snackbar.make(binding.agePicker.rootView, "Gender selected", LENGTH_SHORT)
                .setAnchorView(binding.tvProfile.id).show()
        }
    }
    private suspend fun saveProfile() {
        requireContext().dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("gender")] = binding.radioButton1.isChecked   // male = true; female = false
            preferences[booleanPreferencesKey("athlete")] = binding.switchAthlete.isChecked // athlete = true
            preferences[intPreferencesKey("age")] = binding.agePicker.value
        }
    }
    private fun retrieveProfile() = requireContext().dataStore.data.map { preferences ->
        UserProfile (
            gender = preferences[booleanPreferencesKey("gender")] ?: true,
            athlete = preferences[booleanPreferencesKey("athlete")] ?: false,
            age = preferences[intPreferencesKey("age")] ?: 16,
            heartRate = preferences[intPreferencesKey("heartRate")] ?: 0
                )
    }
}