package com.commit451.gitlab.fragment

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.OnClick
import com.alexgwyn.recyclerviewsquire.DynamicGridLayoutManager
import com.commit451.gitlab.App
import com.commit451.gitlab.R
import com.commit451.gitlab.adapter.GroupMembersAdapter
import com.commit451.gitlab.dialog.AccessDialog
import com.commit451.gitlab.event.MemberAddedEvent
import com.commit451.gitlab.extension.setup
import com.commit451.gitlab.model.api.Group
import com.commit451.gitlab.model.api.Member
import com.commit451.gitlab.navigation.Navigator
import com.commit451.gitlab.rx.CustomResponseSingleObserver
import com.commit451.gitlab.rx.CustomSingleObserver
import com.commit451.gitlab.util.LinkHeaderParser
import com.commit451.gitlab.viewHolder.ProjectMemberViewHolder
import com.trello.rxlifecycle2.android.FragmentEvent
import io.reactivex.Single
import org.greenrobot.eventbus.Subscribe
import org.parceler.Parcels
import retrofit2.Response
import timber.log.Timber

class GroupMembersFragment : ButterKnifeFragment() {

    companion object {

        private val KEY_GROUP = "group"

        fun newInstance(group: Group): GroupMembersFragment {
            val args = Bundle()
            args.putParcelable(KEY_GROUP, Parcels.wrap(group))

            val fragment = GroupMembersFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @BindView(R.id.root) lateinit var root: View
    @BindView(R.id.swipe_layout) lateinit var swipeRefreshLayout: SwipeRefreshLayout
    @BindView(R.id.list) lateinit var list: RecyclerView
    @BindView(R.id.message_text) lateinit var textMessage: TextView
    @BindView(R.id.add_user_button) lateinit var buttonAddUser: View

    lateinit var adapterGroupMembers: GroupMembersAdapter
    lateinit var layoutManagerGroupMembers: DynamicGridLayoutManager

    var member: Member? = null
    lateinit var group: Group
    var nextPageUrl: Uri? = null

    private val mOnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val visibleItemCount = layoutManagerGroupMembers.childCount
            val totalItemCount = layoutManagerGroupMembers.itemCount
            val firstVisibleItem = layoutManagerGroupMembers.findFirstVisibleItemPosition()
            if (firstVisibleItem + visibleItemCount >= totalItemCount && !adapterGroupMembers.isLoading && nextPageUrl != null) {
                loadMore()
            }
        }
    }

    private val listener = object : GroupMembersAdapter.Listener {
        override fun onUserClicked(member: Member, holder: ProjectMemberViewHolder) {
            Navigator.navigateToUser(activity, holder.image, member)
        }

        override fun onUserRemoveClicked(member: Member) {
            this@GroupMembersFragment.member = member
            App.get().gitLab.removeGroupMember(group.id, member.id)
                    .setup(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
                    .subscribe(object : CustomSingleObserver<String>() {

                        override fun error(e: Throwable) {
                            Timber.e(e)
                            Snackbar.make(root, R.string.failed_to_remove_member, Snackbar.LENGTH_SHORT)
                                    .show()
                        }

                        override fun success(value: String) {
                            adapterGroupMembers.removeMember(this@GroupMembersFragment.member!!)
                        }
                    })
        }

        override fun onUserChangeAccessClicked(member: Member) {
            val accessDialog = AccessDialog(activity, member, group)
            accessDialog.setOnAccessChangedListener(object : AccessDialog.OnAccessChangedListener {
                override fun onAccessChanged(member: Member, accessLevel: String) {
                    loadData()
                }
            })
            accessDialog.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        group = Parcels.unwrap<Group>(arguments.getParcelable<Parcelable>(KEY_GROUP))
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_group_members, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        App.bus().register(this)

        adapterGroupMembers = GroupMembersAdapter(listener)
        layoutManagerGroupMembers = DynamicGridLayoutManager(activity)
        layoutManagerGroupMembers.setMinimumWidthDimension(R.dimen.user_list_image_size)
        layoutManagerGroupMembers.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                if (adapterGroupMembers.isFooter(position)) {
                    return layoutManagerGroupMembers.numColumns
                }
                return 1
            }
        }
        list.layoutManager = layoutManagerGroupMembers
        list.adapter = adapterGroupMembers
        list.addOnScrollListener(mOnScrollListener)

        swipeRefreshLayout.setOnRefreshListener { loadData() }

        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        App.bus().unregister(this)
    }

    @OnClick(R.id.add_user_button)
    fun onAddUserClick(fab: View) {
        Navigator.navigateToAddGroupMember(activity, fab, group)
    }

    override fun loadData() {
        if (view == null) {
            return
        }
        if (group == null) {
            swipeRefreshLayout.isRefreshing = false
            return
        }
        textMessage.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = true
        loadGroupMembers(App.get().gitLab.getGroupMembers(group.id))
    }

    private fun loadMore() {
        if (view == null) {
            return
        }

        if (nextPageUrl == null) {
            return
        }

        swipeRefreshLayout.isRefreshing = true
        adapterGroupMembers.isLoading = true

        Timber.d("loadMore called for %s", nextPageUrl)
        loadGroupMembers(App.get().gitLab.getProjectMembers(nextPageUrl!!.toString()))
    }

    private fun loadGroupMembers(observable: Single<Response<List<Member>>>) {
        observable
                .setup(bindUntilEvent(FragmentEvent.DESTROY_VIEW))
                .subscribe(object : CustomResponseSingleObserver<List<Member>>() {

                    override fun error(e: Throwable) {
                        Timber.e(e)
                        swipeRefreshLayout.isRefreshing = false
                        textMessage.visibility = View.VISIBLE
                        textMessage.setText(R.string.connection_error_users)
                        buttonAddUser.visibility = View.GONE
                        adapterGroupMembers.setData(null)
                    }

                    override fun responseNonNullSuccess(members: List<Member>) {
                        swipeRefreshLayout.isRefreshing = false
                        if (members.isEmpty()) {
                            textMessage.visibility = View.VISIBLE
                            textMessage.setText(R.string.no_project_members)
                        }
                        buttonAddUser.visibility = View.VISIBLE
                        if (nextPageUrl == null) {
                            adapterGroupMembers.setData(members)
                        } else {
                            adapterGroupMembers.addData(members)
                        }
                        adapterGroupMembers.isLoading = false

                        nextPageUrl = LinkHeaderParser.parse(response()).next
                        Timber.d("Next page url %s", nextPageUrl)
                    }
                })
    }

    @Subscribe
    fun onMemberAdded(event: MemberAddedEvent) {
        if (adapterGroupMembers != null) {
            adapterGroupMembers.addMember(event.member)
            textMessage.visibility = View.GONE
        }
    }
}