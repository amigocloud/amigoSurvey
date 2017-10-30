package com.amigocloud.amigosurvey.selector

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.amigocloud.amigosurvey.ApplicationScope
import com.amigocloud.amigosurvey.R
import com.amigocloud.amigosurvey.databinding.ActivitySelectorBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import toothpick.Toothpick
import javax.inject.Inject

class SelectorActivity : AppCompatActivity() {

    @Inject lateinit var viewModelFactory: SelectorViewModel.Factory

    private lateinit var binding: ActivitySelectorBinding
    private lateinit var viewModel: SelectorViewModel

    private val adapter = SelectorAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toothpick.openScopes(ApplicationScope::class.java, this).let {
            Toothpick.inject(this, it)
        }
        viewModel = viewModelFactory.get(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_selector)
        binding.list.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.list.adapter = adapter

        viewModel.getProjectList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ adapter.setData(it) }, { error -> })
    }

    override fun onDestroy() {
        super.onDestroy()

        Toothpick.closeScope(this)
    }
}
