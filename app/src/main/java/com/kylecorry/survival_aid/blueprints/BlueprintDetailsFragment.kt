package com.kylecorry.survival_aid.blueprints

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kylecorry.survival_aid.R

class BlueprintDetailsFragment(val blueprint: Blueprint): Fragment() {

    private lateinit var ingredientsList: RecyclerView
    private lateinit var stepsList: RecyclerView
    private lateinit var nameTxt: TextView
    private lateinit var descTxt: TextView
    private lateinit var ingredientAdapter: IngredientAdapter
    private lateinit var stepAdapter: StepAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_blueprint_details, container, false)

        nameTxt = view.findViewById(R.id.blueprint_name)
        descTxt = view.findViewById(R.id.blueprint_description)

        nameTxt.text = blueprint.resource.name
        descTxt.text = blueprint.resource.description

        ingredientsList = view.findViewById(R.id.blueprint_ingredients)
        ingredientsList.layoutManager = LinearLayoutManager(context)

        ingredientAdapter = IngredientAdapter(blueprint.ingredients)
        ingredientsList.adapter = ingredientAdapter

        stepsList = view.findViewById(R.id.blueprint_steps)
        stepsList.layoutManager = LinearLayoutManager(context)

        stepAdapter = StepAdapter(blueprint.steps)
        stepsList.adapter = stepAdapter

        return view
    }

    inner class ResourceHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var nameText: TextView = itemView.findViewById(R.id.resource_name_disp)
        private var quantityText: TextView = itemView.findViewById(R.id.resource_quantity_disp)

        fun bindToResource(resource: Resource, quantity: Quantity){
            nameText.text = resource.name
            quantityText.text = "${quantity.amount} ${quantity.unit}".trim()

            itemView.setOnClickListener {
                val dialog: AlertDialog? = activity?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.apply {
                        setPositiveButton(R.string.dialog_ok) { dialog, id ->
                            // Do nothing
                        }
                        val blueprints = Blueprints.getBlueprintsForResource(resource)
                        var message = resource.description
                        if (blueprints.isNotEmpty()) message += "\n\nBlueprint available!"
                        setMessage(message)
                        setTitle(resource.name)
                    }
                    builder.create()
                }
                dialog?.show()
            }
        }
    }

    inner class IngredientAdapter(mResources: List<Pair<Resource, Quantity>>): RecyclerView.Adapter<ResourceHolder>() {

        var resources = mResources
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResourceHolder {
            val view = layoutInflater.inflate(R.layout.list_item_blueprint_resource, parent, false)
            return ResourceHolder(view)
        }

        override fun getItemCount(): Int {
            return resources.size
        }

        override fun onBindViewHolder(holder: ResourceHolder, position: Int) {
            val resource = resources[position]
            holder.bindToResource(resource.first, resource.second)
        }

    }


    inner class StepHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var stepText: TextView = itemView.findViewById(R.id.blueprint_step_disp)
        private var orderText: TextView = itemView.findViewById(R.id.blueprint_step_number)

        fun bindToStep(step: String, order: Int){
            stepText.text = step
            orderText.text = "${order}."
        }
    }

    inner class StepAdapter(mSteps: List<String>): RecyclerView.Adapter<StepHolder>() {

        var steps = mSteps
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepHolder {
            val view = layoutInflater.inflate(R.layout.list_item_blueprint_step, parent, false)
            return StepHolder(view)
        }

        override fun getItemCount(): Int {
            return steps.size
        }

        override fun onBindViewHolder(holder: StepHolder, position: Int) {
            val step = steps[position]
            holder.bindToStep(step, position + 1)
        }

    }

}

