package com.android.instagramportfolio.view.slide

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.android.instagramportfolio.R
import com.android.instagramportfolio.databinding.ItemSlideBinding
import com.android.instagramportfolio.model.Slide
import com.google.android.flexbox.FlexboxLayoutManager
import java.util.*


class SlideAdapter(
    private val context: Context,
    private val items: MutableList<Slide>,
    private val clickListener: (Slide) -> Unit,
    private val isInstarSize: MutableLiveData<Boolean>,
): RecyclerView.Adapter<SlideAdapter.ViewHolder>(), ItemTouchHelperCallback.OnItemMoveListener {

    private lateinit var dragListener: OnStartDragListener

    inner class ViewHolder(
        val binding: ItemSlideBinding,
    ): RecyclerView.ViewHolder(binding.root), ItemTouchHelperViewHolder {

        private var mySlide: Slide? = null

        private var isFirst: Boolean = false
        private var bindedSlide: Slide? = null
        private var bindedSlideIndex: Int? = null

        fun bind(item: Slide) {
            this.mySlide = item

            binding.imageSlide.setImageBitmap(item.bitmap)
            if (isInstarSize.value == true) {
                binding.imageSlideBefore.setBackgroundResource(R.color.white)
                binding.imageSlide.setBackgroundResource(R.color.white)
                binding.imageSlideAfter.setBackgroundResource(R.color.white)
            } else {
                binding.imageSlideBefore.setBackgroundResource(R.color.gray)
                binding.imageSlide.setBackgroundResource(R.color.gray)
                binding.imageSlideAfter.setBackgroundResource(R.color.gray)
            }

            //  바인딩 되어있는 놈들이면 배경에 보더 추가
            if (isBindingContains(item)) {
//                binding.layoutSlideWrapper.setBackgroundResource(R.drawable.border_without_none)

                for ((first, second) in bindingPairs) {
                    if (first == item) {
                        binding.layoutSlideWrapper.foreground = ContextCompat.getDrawable(context, R.drawable.icon_check)
                        break
                    }
                    if (second == item) {
                        binding.layoutSlideWrapper.foreground =  ColorDrawable(Color.TRANSPARENT)
                        break
                    }
                }
            }
            else {
//                binding.layoutSlideWrapper.setBackgroundResource(Color.TRANSPARENT)
                binding.layoutSlideWrapper.foreground =  ColorDrawable(Color.TRANSPARENT)
            }


            binding.root.setOnClickListener {
                clickListener(item)
            }

            binding.root.setOnLongClickListener {
                dragListener.onStartDrag(this)
                return@setOnLongClickListener false
            }
        }

        // 아이템 드래그가 시작함
        override fun onItemSelected() {
            if (mySlide == null) return

            if (isBindingContains(mySlide!!)) {

                val myIndex = items.indexOf(mySlide)

                val params = binding.root.layoutParams as FlexboxLayoutManager.LayoutParams
                params.apply {
                    flexGrow = 1.0f
                }

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

                        binding.imageSlideAfter.foreground = ColorDrawable(Color.TRANSPARENT)
                        // 위엣 놈 추가됨

                        binding.imageSlideAfter.visibility = View.VISIBLE
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

                        binding.imageSlideBefore.foreground = ContextCompat.getDrawable(context, R.drawable.icon_check)
                        // 위엣 놈 추가됨

                        binding.imageSlideBefore.visibility = View.VISIBLE
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
                binding.imageSlideBefore.visibility = View.GONE
                binding.imageSlideAfter.visibility = View.GONE

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
                    Log.d("SlideAdapter", "myIndex: $myIndex")
                    // 내 슬라이드가 뒤에 있을 때
                    val prevSlide = bindedSlide!!

                    binding.imageSlideBefore.foreground = ColorDrawable(Color.TRANSPARENT)
                    // 위엣 놈 추가됨

                    if (myIndex <= 0) {
                        items.add(0, prevSlide)
                        notifyItemInserted(0)
                    } else {
                        items.add(myIndex, prevSlide)
                        notifyItemInserted(myIndex)
                    }
                }
            }

//            setBindingPairsBackground()
            notifyDataSetChanged()
        }
    }

    // 뷰 홀더 관리
    private val viewHolders = mutableMapOf<Slide, ViewHolder>()

    // 바인딩 관리
    var bindingPairs = mutableListOf<Pair<Slide, Slide>>()
    val bindingFlattenSlides get() = bindingPairs.flatMap { it.toList() }

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

    fun replaceItems(items: List<Slide>, bindingParis: MutableList<Pair<Slide, Slide>>) {
        this.viewHolders.clear()
        this.bindingPairs.clear()
        this.items.clear()
        this.items.addAll(items)
        this.bindingPairs.clear()
        this.bindingPairs.addAll(bindingParis)
        notifyDataSetChanged()
    }

    // 이미지가 화면에 꽉차게 확장시키기
    fun setOriginalImage() {
        for ((_, holder) in viewHolders) {
            holder.binding.imageSlideBefore.setBackgroundResource(R.color.gray)
            holder.binding.imageSlide.setBackgroundResource(R.color.gray)
            holder.binding.imageSlideAfter.setBackgroundResource(R.color.gray)
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
            viewHolders[first]?.binding?.layoutSlideWrapper?.foreground = ContextCompat.getDrawable(context, R.drawable.icon_check)
            viewHolders[second]?.binding?.layoutSlideWrapper?.foreground = ColorDrawable(Color.TRANSPARENT)
//
//            viewHolders[first]?.binding?.layoutSlideWrapper?.setBackgroundResource(R.drawable.border_without_none)
//            viewHolders[second]?.binding?.layoutSlideWrapper?.setBackgroundResource(R.drawable.border_without_none)
        }
    }

    // 바인딩 취소
    fun cancelBinding(slide: Slide) {
        for ((first, second) in bindingPairs) {
            // 슬라이드가 바인딩 페어에 등록되어 있다면 취소 시킴
            if (first == slide || second == slide) {
                bindingPairs.remove(first to second)

                viewHolders[first]?.binding?.layoutSlideWrapper?.foreground = ColorDrawable(Color.TRANSPARENT)
                viewHolders[second]?.binding?.layoutSlideWrapper?.foreground = ColorDrawable(Color.TRANSPARENT)
//                viewHolders[first]?.binding?.layoutSlideWrapper?.setBackgroundResource(Color.TRANSPARENT)
//                viewHolders[second]?.binding?.layoutSlideWrapper?.setBackgroundResource(Color.TRANSPARENT)
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
                    toPosition2++
                    break
                }
            }

            for (i in fromPosition downTo toPosition2 + 1) {
                Collections.swap(items, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition2)
    }

    override fun onItemSwiped(position: Int) {
        // TODO("Not yet implemented")
    }
}