package com.codinlog.album.controller.fragment.kotlin

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.codinlog.album.R
import com.codinlog.album.adapter.kotlin.DiaryAdapter
import com.codinlog.album.application.AlbumApplication
import com.codinlog.album.controller.BaseFragmentController
import com.codinlog.album.controller.activity.kotlin.DiaryPublishActivity
import com.codinlog.album.databinding.DiaryFragmentBinding
import com.codinlog.album.entity.kotlin.DiaryEntity
import com.codinlog.album.listener.CommonListener
import com.codinlog.album.model.kotlin.DiaryViewModel
import com.codinlog.album.network.kotlin.model.UserModel
import com.google.android.material.bottomsheet.BottomSheetBehavior

class DiaryFragment : BaseFragmentController<DiaryViewModel, DiaryFragmentBinding>() {
    private lateinit var diaryAdapter: DiaryAdapter
    private lateinit var sheetBehavior: BottomSheetBehavior<View>
    private lateinit var ivHeader: ImageView
    private lateinit var tvName: TextView
    private lateinit var layout: ConstraintLayout
    private lateinit var rvBottom: RecyclerView
    private val handler = Handler()
    override fun getLayoutId(): Int {
        return R.layout.diary_fragment
    }

    override fun doInitViewData() {
        viewModel = ViewModelProvider(this).get(DiaryViewModel::class.java)
        sheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        ivHeader = binding.root.findViewById(R.id.ivHeader)
        tvName = binding.root.findViewById(R.id.tvName)
        rvBottom = binding.root.findViewById(R.id.rvBottom)
        layout = binding.root.findViewById(R.id.layout)
        binding.rv.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun doInitListener() {
        layout.setOnClickListener {
            val view = LayoutInflater.from(context).inflate(R.layout.user_login, null)
            val etName = view.findViewById<EditText>(R.id.etName)
            val etPassword = view.findViewById<EditText>(R.id.etPassword)
            val builder = AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setTitle(R.string.user_login)
                    .setView(view)
                    .setNegativeButton(R.string.btn_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.btn_ok, null)
                    .create()
            builder.setOnShowListener {
                val button = builder.getButton(AlertDialog.BUTTON_POSITIVE)
                button.setOnClickListener {
                    val username = etName.text.toString()
                    val password = etPassword.text.toString()
                    viewModel.login(UserModel(username, null, password, null))
                }
                etName.requestFocus()
                val inputManager = AlbumApplication.context
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                handler.postDelayed(Runnable { inputManager.showSoftInput(etName, 0) }, 200)
            }
            builder.show()
        }
        activity?.let {
            viewModel.user.observe(it, Observer { userInfo ->
                userInfo?.let {
                    tvName.text = userInfo.username
                    Toast.makeText(context, R.string.user_login_success, Toast.LENGTH_SHORT).show()
                }
            })
        }
        binding.floatBtn.setOnClickListener {
            val intent = Intent(context, DiaryPublishActivity::class.java)
            startActivity(intent)
        }

        activity?.let { t ->
            viewModel.displayData.observe(t, Observer { i ->
                diaryAdapter.displayData = i
                binding.rv.scrollToPosition(0)
            })
        }

        binding.rv.setOnTouchListener { _, _ ->
            if (sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            false
        }

        binding.rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE)
                    binding.floatBtn.visibility = View.VISIBLE
                else
                    binding.floatBtn.visibility = View.INVISIBLE
            }
        })
    }

    override fun doInitDisplayData() {
        diaryAdapter = DiaryAdapter(CommonListener {
            val pairs = it as Pair<ImageButton, DiaryEntity>
            val imgBtn = pairs.first
            val diaryEntity = pairs.second
            val popupMenu = PopupMenu(context, imgBtn)
            popupMenu.inflate(R.menu.delete_menu)
            popupMenu.setOnMenuItemClickListener { i ->
                if (i.itemId == R.id.delete)
                    viewModel.deleteDiary(diaryEntity)
                true
            }
            popupMenu.show()
        })
        binding.rv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.rv.adapter = diaryAdapter
        viewModel.login(null)
    }

    companion object {
        @JvmStatic
        fun newInstance(): Fragment {
            return DiaryFragment()
        }
    }
}