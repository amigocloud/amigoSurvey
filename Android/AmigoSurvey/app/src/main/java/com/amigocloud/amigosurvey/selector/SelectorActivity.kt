package com.amigocloud.amigosurvey.selector

import android.arch.lifecycle.Observer
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.amigocloud.amigosurvey.ApplicationScope
import com.amigocloud.amigosurvey.R
import com.amigocloud.amigosurvey.databinding.ActivitySelectorBinding
import com.amigocloud.amigosurvey.form.FormActivity
import toothpick.Toothpick
import javax.inject.Inject

class SelectorActivity : AppCompatActivity() {

    @Inject lateinit var viewModelFactory: SelectorViewModel.Factory
    private lateinit var viewModel: SelectorViewModel
    private lateinit var binding: ActivitySelectorBinding

    private val adapter: SelectorAdapter = SelectorAdapter({
        if (it.type == SelectorItem.Type.PROJECT) {
            viewModel.selectedProject.set(it)
            viewModel.showDatasets()
        } else {
            val intent = Intent(this, FormActivity::class.java)
            intent.putExtra(FormActivity.INTENT_USER_ID, 2L)
            viewModel.selectedProject.get()?.let { project_id ->
                intent.putExtra(FormActivity.INTENT_PROJECT_ID, project_id.id)
            }
            intent.putExtra(FormActivity.INTENT_DATASET_ID, it.id)
            startActivity(intent)
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Toothpick.openScopes(ApplicationScope::class.java, this).let {
            Toothpick.inject(this, it)
        }
        viewModel = viewModelFactory.get(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_selector)
        binding.list.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.list.adapter = adapter

        viewModel.dataSource.observe(this@SelectorActivity, Observer { list -> adapter.setList(list) })
    }

    override fun onDestroy() {
        super.onDestroy()

        Toothpick.closeScope(this)
    }

    override fun onBackPressed() {
        if (viewModel.selectedProject.get() != null) {
            viewModel.selectedProject.set(null)
            viewModel.showProjects()
            return
        }
        super.onBackPressed()
    }
}
