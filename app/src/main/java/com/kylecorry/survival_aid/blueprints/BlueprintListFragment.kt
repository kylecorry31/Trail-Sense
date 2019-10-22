package com.kylecorry.survival_aid.blueprints

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kylecorry.survival_aid.R
import com.kylecorry.survival_aid.doTransaction

class BlueprintListFragment: Fragment() {

    private lateinit var blueprintList: RecyclerView
    private lateinit var adapter: BlueprintAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_blueprint_list, container, false)

        blueprintList = view.findViewById(R.id.blueprint_recycler)

        blueprintList.layoutManager = LinearLayoutManager(context)

        adapter = BlueprintAdapter(Blueprints.blueprints.sortedWith(compareBy({ it.category.name }, { it.resource.name })))
        blueprintList.adapter = adapter

        return view
    }

    inner class BlueprintHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var nameText: TextView = itemView.findViewById(R.id.blueprint_name_disp)
        private var categoryText: TextView = itemView.findViewById(R.id.blueprint_category_disp)

        fun bindToBlueprint(blueprint: Blueprint){
            nameText.text = blueprint.resource.name
            categoryText.text = blueprint.category.name.toLowerCase().capitalize()

            itemView.setOnClickListener {
                fragmentManager?.doTransaction {
                    this.addToBackStack(null)
                    this.replace(R.id.fragment_holder, BlueprintDetailsFragment(blueprint))
                }
            }
        }
    }

    inner class BlueprintAdapter(mBlueprints: List<Blueprint>): RecyclerView.Adapter<BlueprintHolder>() {

        var blueprints = mBlueprints
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlueprintHolder {
            val view = layoutInflater.inflate(R.layout.list_item_blueprint, parent, false)
            return BlueprintHolder(view)
        }

        override fun getItemCount(): Int {
            return blueprints.size
        }

        override fun onBindViewHolder(holder: BlueprintHolder, position: Int) {
            val blueprint = blueprints[position]
            holder.bindToBlueprint(blueprint)
        }

    }

}

