/*
 * Copyright (c) 2020 Proton Technologies AG
 *
 * This file is part of ProtonVPN.
 *
 * ProtonVPN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonVPN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonVPN.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.protonvpn.android.tv.detailed

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.transition.ChangeBounds
import androidx.transition.ChangeClipBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.ChangeTransform
import androidx.transition.Fade
import androidx.transition.Slide
import androidx.transition.TransitionSet
import com.protonvpn.android.R
import com.protonvpn.android.components.BaseFragmentV2
import com.protonvpn.android.components.ContentLayout
import com.protonvpn.android.databinding.FragmentTvCountryDetailsBinding
import com.protonvpn.android.tv.models.CountryCard
import com.protonvpn.android.ui.home.TvHomeViewModel
import javax.inject.Inject

@ContentLayout(R.layout.fragment_tv_country_details)
class CountryDetailFragment : BaseFragmentV2<TvHomeViewModel, FragmentTvCountryDetailsBinding>() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun initViewModel() {
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(TvHomeViewModel::class.java)
    }

    lateinit var card: CountryCard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = TransitionSet().apply {
            addTransition(Fade(Fade.OUT))
            addTransition(Slide())
            addTransition(Fade(Fade.IN))
        }

        sharedElementEnterTransition = TransitionSet().apply {
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
            addTransition(ChangeImageTransform())
            addTransition(ChangeClipBounds())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
    }

    private val focusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
        v.pivotX = v.width / 2f
        v.pivotY = v.height / 2f
        v.animate().scaleX(if (hasFocus) 1.1f else 1f)
        v.animate().scaleY(if (hasFocus) 1.1f else 1f)
    }

    private fun setupUi() {
        val extras = arguments
        if (extras != null && extras.containsKey(EXTRA_CARD)) {
            card = extras[EXTRA_CARD] as CountryCard
        }

        postponeEnterTransition()
        binding.countryName.text = card.title

        binding.flag.transitionName = transitionNameForCountry(card.vpnCountry.flag)
        binding.flag.setImageResource(card.image)
        binding.flag.doOnPreDraw {
            startPostponedEnterTransition()
        }

        binding.defaultConnection.onFocusChangeListener = focusChangeListener
        binding.defaultConnection.isChecked = viewModel.isDefaultCountry(card.vpnCountry)
        binding.defaultConnection.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAsDefaultCountry(isChecked, card.vpnCountry)
        }

        binding.connectStreaming.onFocusChangeListener = focusChangeListener
        binding.connectStreaming.setOnClickListener {
            Toast.makeText(context, "Not yet implemented", Toast.LENGTH_SHORT).show()
        }

        binding.connectFastest.onFocusChangeListener = focusChangeListener
        binding.connectFastest.setOnClickListener {
            viewModel.connect(requireActivity(), card)
        }

        binding.disconnect.onFocusChangeListener = focusChangeListener
        binding.disconnect.setOnClickListener {
            viewModel.disconnect()
        }

        viewModel.vpnStateMonitor.vpnStatus.observe(viewLifecycleOwner, Observer {
            val showConnectButtons = !viewModel.isConnectedToCountry(card)

            with(binding) {
                if (connectStreaming.isVisible != showConnectButtons) {
                    connectFastest.isVisible = showConnectButtons
                    connectStreaming.isVisible = showConnectButtons
                    if (showConnectButtons) {
                        if (viewModel.haveAccessToStreaming)
                            connectStreaming.requestFocus()
                        else
                            connectFastest.requestFocus()
                    }
                    disconnect.isVisible = !showConnectButtons
                }
            }
        })
    }

    companion object {
        fun transitionNameForCountry(code: String) = "transition_$code"

        const val EXTRA_CARD = "card"
    }
}