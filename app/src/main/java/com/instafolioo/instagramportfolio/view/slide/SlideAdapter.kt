package com.instafolioo.instagramportfolio.view.slide

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.instafolioo.instagramportfolio.R
import com.instafolioo.instagramportfolio.databinding.ItemSlideBinding
import com.instafolioo.instagramportfolio.model.Slide
import java.util.*


class SlideAdapter(
    private val context: Context,
    val items: MutableList<Slide>,
    private val clickListener: (Slide) -> Unit,
    private val viewModel: SlideViewModel,
): RecyclerView.Adapter<SlideAdapter.ViewHolder>(), ItemTouchHelperCallback.OnItemMoveListener {

    private lateinit var dragListener: OnStartDragListener

    private val transparentDrawable = ColorDrawable(Color.TRANSPARENT)
    private val borderWithoutLeft = ContextCompat.getDrawable(context, R.drawable.border_without_left)
    private val borderWithoutRight = ContextCompat.getDrawable(context, R.drawable.border_without_right)

    inner class ViewHolder(
        val binding: ItemSlideBinding,
    ): RecyclerView.ViewHolder(binding.root), ItemTouchHelperViewHolder {

        private var mySlide: Slide? = null

        private var isFirst: Boolean = false
        private var bindedSlide: Slide? = null
        private var bindedSlideIndex: Int? = null

        fun bind(item: Slide) {
            this.mySlide = item

            setSlideSize()

            binding.imageSlide.setImageBitmap(item.bitmap)
            // 인스타 사이즈
            if (viewModel.isInstarSize.value == true) {
                binding.imageSlideBefore.setBackgroundResource(R.color.white)
                binding.imageSlide.setBackgroundResource(R.color.white)
                binding.imageSlideAfter.setBackgroundResource(R.color.white)
            }
            // 원본 사이즈
            else {
                binding.imageSlideBefore.setBackgroundResource(android.R.color.transparent)
                binding.imageSlide.setBackgroundResource(android.R.color.transparent)
                binding.imageSlideAfter.setBackgroundResource(android.R.color.transparent)
            }

            //  바인딩 되어있는 놈들이면 배경에 보더 추가
            if (isBindingContains(item)) {

                for ((first, second) in bindingPairs) {
                    // 이 아이템이 처음 놈일 때
                    if (first == item) {
                        binding.borderConnectStartTop.visibility = View.GONE
                        binding.borderConnectStartBottom.visibility = View.GONE
                        binding.wrapperBorderImageSlide.foreground = borderWithoutRight
                        binding.borderConnectEndTop.visibility = View.VISIBLE
                        binding.borderConnectEndBottom.visibility = View.VISIBLE
                        break
                    }
                    // 이 아이템이 두 번쨰 놈일 때
                    if (second == item) {
                        binding.borderConnectStartTop.visibility = View.VISIBLE
                        binding.borderConnectStartBottom.visibility = View.VISIBLE
                        binding.wrapperBorderImageSlide.foreground = borderWithoutLeft
                        binding.borderConnectEndTop.visibility = View.GONE
                        binding.borderConnectEndBottom.visibility = View.GONE
                        break
                    }
                }
            }
            // 이 놈이 바인딩 되어있지 않았을 때
            else {
                binding.borderConnectStartTop.visibility = View.GONE
                binding.borderConnectStartBottom.visibility = View.GONE
                binding.wrapperBorderImageSlide.foreground = transparentDrawable
                binding.borderConnectEndTop.visibility = View.GONE
                binding.borderConnectEndBottom.visibility = View.GONE
            }


            binding.root.setOnClickListener {
                clickListener(item)
            }

            binding.root.setOnLongClickListener {
                dragListener.onStartDrag(this)
                return@setOnLongClickListener false
            }
        }

        // 슬라이드 크기가 화면에 맞게 조절
        fun setSlideSize() {
            binding.apply {

                layoutBefore.layoutParams.apply {
                    width = slideSize
                    height = slideSize
                }
                layoutBorderBefore.layoutParams.apply {
                    width = slideSize - 2
                    height = slideSize - 2
                }
                imageSlideBefore.layoutParams.apply {
                    width = slideSize - 10
                    height = slideSize - 10
                }

                layoutCenter.layoutParams.apply {
                    width = slideSize
                    height = slideSize
                }
                wrapperBorderImageSlide.layoutParams.apply {
                    width = slideSize - 2
                    height = slideSize - 2
                }
                imageSlide.layoutParams.apply {
                    width = slideSize - 10
                    height = slideSize - 10
                }

                layoutAfter.layoutParams.apply {
                    width = slideSize
                    height = slideSize
                }
                layoutBorderAfter.layoutParams.apply {
                    width = slideSize - 2
                    height = slideSize - 2
                }
                imageSlideAfter.layoutParams.apply {
                    width = slideSize - 10
                    height = slideSize - 10
                }

                layoutBefore.requestLayout()
                layoutBorderBefore.requestLayout()
                imageSlideBefore.requestLayout()

                layoutCenter.requestLayout()
                wrapperBorderImageSlide.requestLayout()
                imageSlide.requestLayout()

                layoutAfter.requestLayout()
                layoutBorderAfter.requestLayout()
                imageSlideAfter.requestLayout()
            }
        }


        // 아이템 드래그가 시작함
        override fun onItemSelected() {
            if (mySlide == null) return

            if (isBindingContains(mySlide!!)) {

                val myIndex = items.indexOf(mySlide)

                // 이 슬라이드가 첫번째인지 두번째인지 체크
                for ((first, second) in bindingPairs) {

                    // 이 슬라이드가 첫번째
                    if (first == mySlide) {
                        val nextSlideIndex = myIndex + 1
                        val nextSlide = getSlideAt(nextSlideIndex)

                        items.remove(nextSlide)
                        viewHolders.remove(nextSlide)
                        notifyItemRemoved(nextSlideIndex)

                        isFirst = true
                        bindedSlide = nextSlide
                        bindedSlideIndex = nextSlideIndex

                        binding.layoutAfter.visibility = View.VISIBLE
                        binding.imageSlideAfter.setImageBitmap(nextSlide.bitmap)
                        break
                    }
                    // 내 슬라이드가 두번째
                    else if (second == mySlide) {
                        val prevSlideIndex = myIndex - 1
                        val prevSlide = getSlideAt(prevSlideIndex)

                        items.remove(prevSlide)
                        viewHolders.remove(prevSlide)
                        notifyItemRemoved(prevSlideIndex)

                        isFirst = false
                        bindedSlide = prevSlide
                        bindedSlideIndex = prevSlideIndex

                        binding.layoutBefore.visibility = View.VISIBLE
                        binding.imageSlideBefore.setImageBitmap(prevSlide.bitmap)
                        break
                    }
                }
            }
       }

        // 아이템 드래그가 끝남
        override fun onItemClear() {
            if (isBindingContains(mySlide!!)) {
                // 옆의 따까리 안 보이게 비활성화
                binding.layoutBefore.visibility = View.GONE
                binding.layoutAfter.visibility = View.GONE

                val myIndex = items.indexOf(mySlide)

                // 바인딩 되어있는 아이템도 같이 이동
                if (isFirst) {
                    // 내 슬라이드가 앞에 있을 때
                    val nextSlide = bindedSlide!!

                    if (myIndex + 1 >= items.size) {
                        items.add(nextSlide)
                        notifyItemInserted(itemCount - 1)
                    } else {
                        items.add(myIndex + 1, nextSlide)
                        notifyItemInserted(myIndex + 1)
                    }
                }
                else {
                    // 내 슬라이드가 뒤에 있을 때
                    val prevSlide = bindedSlide!!

                    if (myIndex <= 0) {
                        items.add(0, prevSlide)
                        notifyItemInserted(0)
                    } else {
                        items.add(myIndex, prevSlide)
                        notifyItemInserted(myIndex)
                    }
                }
            }

            notifyDataSetChanged()
        }
    }

    // 뷰 홀더 관리
    private val viewHolders = mutableMapOf<Slide, ViewHolder>()

    // 바인딩 관리
    var bindingPairs = mutableListOf<Pair<Slide, Slide>>()
    val bindingFlattenSlides get() = bindingPairs.flatMap { it.toList() }

    var slideSize = 80.dp

    // dp to px
    private val Int.dp: Int
    get() {
        val scale = context.resources.displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: ItemSlideBinding = DataBindingUtil.inflate(inflater, R.layout.item_slide, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        viewHolders[item] = holder
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun getIndexOf(slide: Slide) = items.indexOf(slide)

    fun getSlideAt(index: Int) = items[index]

    fun isBindingContains(slide: Slide) = slide in bindingFlattenSlides


    fun setSlidesSize(size: Int) {
        slideSize = size

        for ((_, holder) in viewHolders) {
            holder.setSlideSize()
        }
    }

    fun replaceItems(items: List<Slide>, bindingParis: MutableList<Pair<Slide, Slide>>) {
        this.viewHolders.clear()
        this.bindingPairs.clear()
        this.items.clear()
        this.items.addAll(items)
        this.bindingPairs.clear()
        this.bindingPairs.addAll(bindingParis)
        notifyDataSetChanged()
    }

    // 이미지 원본으로 변환
    fun setOriginalImage() {
        for ((_, holder) in viewHolders) {
            holder.binding.imageSlideBefore.setBackgroundResource(android.R.color.transparent)
            holder.binding.imageSlide.setBackgroundResource(android.R.color.transparent)
            holder.binding.imageSlideAfter.setBackgroundResource(android.R.color.transparent)
        }
    }

    // 이미지 인스타 사이즈로 변환
    fun setImagesInstarSize() {
        for ((_, holder) in viewHolders) {
            holder.binding.imageSlideBefore.setBackgroundResource(R.color.white)
            holder.binding.imageSlide.setBackgroundResource(R.color.white)
            holder.binding.imageSlideAfter.setBackgroundResource(R.color.white)
        }
    }


    // 바인딩 등록
    fun registerBinding(first: Slide, second: Slide) {
        val pair = first to second
        if (pair !in bindingPairs) {
            bindingPairs.add(pair)
            // 앞에 있는 슬라이드
            viewHolders[first]?.binding?.run {
                borderConnectStartTop.visibility = View.GONE
                borderConnectStartBottom.visibility = View.GONE
                wrapperBorderImageSlide.foreground = borderWithoutRight
                borderConnectEndTop.visibility = View.VISIBLE
                borderConnectEndBottom.visibility = View.VISIBLE
            }

            // 뒤에 있는 슬라이드
            viewHolders[second]?.binding?.run {
                borderConnectStartTop.visibility = View.VISIBLE
                borderConnectStartBottom.visibility = View.VISIBLE
                wrapperBorderImageSlide.foreground = borderWithoutLeft
                borderConnectEndTop.visibility = View.GONE
                borderConnectEndBottom.visibility = View.GONE
            }
        }
    }

    // 바인딩 취소
    fun cancelBinding(slide: Slide) {
        for ((first, second) in bindingPairs) {
            // 슬라이드가 바인딩 페어에 등록되어 있다면 취소 시킴
            if (first == slide || second == slide) {
                bindingPairs.remove(first to second)

                // 앞에 있는 슬라이드
                viewHolders[first]?.binding?.run {
                    borderConnectStartTop.visibility = View.GONE
                    borderConnectStartBottom.visibility = View.GONE
                    wrapperBorderImageSlide.foreground = transparentDrawable
                    borderConnectEndTop.visibility = View.GONE
                    borderConnectEndBottom.visibility = View.GONE
                }
                // 뒤에 있는 슬라이드
                viewHolders[second]?.binding?.run {
                    borderConnectStartTop.visibility = View.GONE
                    borderConnectStartBottom.visibility = View.GONE
                    wrapperBorderImageSlide.foreground = transparentDrawable
                    borderConnectEndTop.visibility = View.GONE
                    borderConnectEndBottom.visibility = View.GONE
                }
                break
            }
        }
    }



    interface OnStartDragListener {
        fun onStartDrag(viewHolder: ViewHolder)
    }

    fun startDrag(listener: OnStartDragListener) {
        this.dragListener = listener
    }

    // 드래그 할때마다 이동
    override fun onItemMoved(fromPosition: Int, toPosition: Int) {

        // 이동된 놈이 바인딩 중간에 끼지 못하도록 조정
        var toPosition2 = toPosition

        if (fromPosition < toPosition) {
            for ((first, _) in bindingPairs) {
                val firstIndex = getIndexOf(first)
                if (toPosition == firstIndex) {
                    toPosition2++
                    break
                }
            }

            for (i in fromPosition until toPosition2) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for ((_, second) in bindingPairs) {
                val secondIndex = getIndexOf(second)
                if (toPosition == secondIndex) {
                    toPosition2--
                    if (toPosition < 0)
                        toPosition2 = 0
                    break
                }
            }

            for (i in fromPosition downTo toPosition2 + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition2)

        if (fromPosition != toPosition2) {
            viewModel.isSlideChanged.value = true
        }
    }

    override fun onItemSwiped(position: Int) {
        // TODO("Not yet implemented")
    }
}