---
name: trail-sense-groupable-support
description: Add groupable support to a Trail Sense feature or model. Use when asked to make an item groupable, add folders/groups, add group navigation, or retrofit an existing list using IGroupable/Groupable, GroupLoader, GroupListManager, group pickers, and Room parent/group persistence.
---

# Trail Sense Groupable Support

Use this skill when adding group/folder support to an existing Trail Sense item type.

Primary examples to inspect first:
- Photo Maps: `tools/photo_maps/domain/IMap.kt`, `MapGroup.kt`, `MapService.kt`, `MapGroupLoader.kt`, `MapPickers.kt`, `PhotoMapListFragment.kt`, `ui/mappers/*`
- Paths: `tools/paths/domain/IPath.kt`, `PathGroup.kt`, `PathService.kt`, `PathGroupLoader.kt`, `PathPickers.kt`, `PathsFragment.kt`, `ui/*Group*`
- Beacons: `tools/beacons/domain/IBeacon.kt`, `BeaconGroup.kt`, `BeaconService.kt`, `BeaconLoader.kt`, `BeaconPickers.kt`, `BeaconListFragment.kt`
- Offline Maps: `tools/map/domain/IOfflineMapFile.kt`, `OfflineMapFileGroup.kt`, `OfflineMapFileService.kt`, `OfflineMapFileGroupLoader.kt`, `OfflineMapFilePickers.kt`, `OfflineMapListFragment.kt`

## Core Checklist

1. Create an interface for the item extending `shared.grouping.Groupable`.
2. Make the item implement the interface with `isGroup = false`, `count = null`, and `parentId`.
3. Create a group domain class implementing the same interface with `isGroup = true`.
4. Add a nullable parent column to the item Room entity and mapping.
5. Add a Room entity and DAO for groups.
6. Add repo methods for item/group persistence and group queries.
7. Add a service with `GroupLoader`, `GroupCounter`, and usually `GroupDeleter`.
8. Add an `ISearchableGroupLoader` using `GroupFilter`.
9. Add list item mappers for item, group, and interface dispatch.
10. Update the list UI to use `GroupListManager`.
11. Add group and item/group pickers if moving/selecting is needed.
12. Add create group, move, delete, rename, and group visibility actions as needed.
13. Add Room migration and register new entities/DAOs in `AppDatabase`.
14. Register new repos/services in the tool registration singleton list when appropriate.
15. Compile with `./gradlew :app:compileDebugKotlin`.

## Domain Pattern

Interface:

```kotlin
interface IThing : Groupable {
    val name: String
}
```

Item:

```kotlin
data class Thing(
    override val id: Long,
    override val name: String,
    override val parentId: Long? = null,
    // existing fields
) : IThing {
    override val isGroup = false
    override val count: Int? = null
}
```

Group:

```kotlin
data class ThingGroup(
    override val id: Long,
    override val name: String,
    override val parentId: Long? = null,
    override val count: Int? = 0
) : IThing {
    override val isGroup = true
}
```

Use `parent` as the DB column name when matching Photo Maps/Paths/Offline Maps unless the feature already has a different convention.

## Persistence Pattern

Item DAO needs:

```kotlin
@Query("SELECT * FROM things WHERE parent IS :parent")
suspend fun getAllWithParent(parent: Long?): List<ThingEntity>
```

Group DAO:

```kotlin
@Dao
interface ThingGroupDao {
    @Query("SELECT * FROM thing_groups WHERE parent IS :parent")
    suspend fun getAllWithParent(parent: Long?): List<ThingGroupEntity>

    @Query("SELECT * FROM thing_groups WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): ThingGroupEntity?

    @Upsert
    suspend fun upsert(group: ThingGroupEntity): Long

    @Delete
    suspend fun delete(group: ThingGroupEntity)
}
```

Prefer `@Upsert` over separate insert/update for new group DAO code.

Migration requirements:
- Increment `AppDatabase` version by one.
- Add `parent` to the item table if missing.
- Create group table.
- Create indices matching entity annotations, commonly:
  - `index_{item_table}_parent`
  - `index_{group_table}_parent`
- Add group entity and DAO accessor to `AppDatabase`.

## Service Pattern

Create or extend the feature service:

```kotlin
class ThingService {
    private val repo = getAppService<ThingRepo>()

    val loader = GroupLoader(this::getGroup, this::getChildren)
    private val counter = GroupCounter(loader)

    private val deleter = object : GroupDeleter<IThing>(loader) {
        override suspend fun deleteItems(items: List<IThing>) {
            items.filterIsInstance<Thing>().forEach { repo.delete(it) }
        }

        override suspend fun deleteGroup(group: IThing) {
            repo.deleteGroup(group as ThingGroup)
        }
    }

    suspend fun add(item: IThing): Long {
        return if (item.isGroup) repo.addGroup(item as ThingGroup) else repo.add(item as Thing)
    }

    suspend fun delete(item: IThing) {
        deleter.delete(item)
    }

    suspend fun getGroup(id: Long?): ThingGroup? {
        id ?: return null
        return repo.getGroup(id)?.copy(count = counter.count(id))
    }

    private suspend fun getChildren(parentId: Long?): List<IThing> {
        return repo.getItemsWithParent(parentId) + getGroups(parentId)
    }

    private suspend fun getGroups(parentId: Long?): List<ThingGroup> {
        return repo.getGroupsWithParent(parentId).map { it.copy(count = counter.count(it.id)) }
    }
}
```

If the feature already has a service, integrate rather than duplicating it. If the service is a tool singleton, register it in `{Tool}ToolRegistration.singletons` and use `getAppService<Service>()`.

## Search Loader Pattern

```kotlin
class ThingGroupLoader(private val loader: IGroupLoader<IThing>) : ISearchableGroupLoader<IThing> {
    private val filter = GroupFilter(loader)

    override suspend fun getGroup(id: Long): IThing? = loader.getGroup(id)

    override suspend fun load(search: String?, group: Long?): List<IThing> {
        return if (search.isNullOrBlank()) {
            loader.getChildren(group, 1)
        } else {
            filter.filter(group) { it.name.contains(search, ignoreCase = true) }
        }
    }
}
```

Use `maxDepth = 1` for normal group list navigation and unrestricted depth only for recursive operations such as group visibility or counting.

## UI Pattern

Respect existing fragments using `TrailSenseReactiveFragment`, and follow the local reactive style: do not add fragment-level cached variables for repo/service/loader/manager. Create services, loaders, managers, and mappers inside `update()` with hooks. Do not define local action/helper functions inside `update()`. Keep action/helper functions as private fragment methods outside `update()`, and pass hook-owned values such as `service` and `manager` as parameters.

```kotlin
override fun update() {
    val context = useAndroidContext()
    val repo = useService<ThingRepo>()
    val service = useMemo { getAppService<ThingService>() }
    val loader = useMemo(service) { ThingGroupLoader(service.loader) }
    val manager = useMemo(loader) {
        GroupListManager(lifecycleScope, loader, null, this::sortThings)
    }

    val mapper = useMemo(context, manager, service) {
        IThingListItemMapper(
            requireContext(),
            { item, action -> handleListItemAction(item, action, service, manager) },
            { group, action -> handleGroupAction(group, action, service, manager) }
        )
    }

    // Bind SearchView and list/title via GroupListManager.bind().
    // Observe repo flow or service change key and call manager.refresh().
    // Use useBackPressedCallback(manager) { manager.up() }.
}

private fun handleListItemAction(
    item: Thing,
    action: ThingAction,
    service: ThingService,
    manager: GroupListManager<IThing>
) {
    when (action) {
        ThingAction.View -> view(item)
        ThingAction.Rename -> rename(item, service, manager)
        ThingAction.Move -> move(item, service, manager)
        ThingAction.Delete -> delete(item, service, manager)
    }
}
```

For `BoundFragment` features, mirror Photo Maps/Paths directly with a `private lateinit var manager`; do not convert a fragment type unless the user asks or the existing feature requires it.

List mappers:
- Item mapper handles normal actions plus `Move`.
- Group mapper uses `ic_map_group` or an existing group icon, shows `R.plurals.*_group_summary`, and includes `Rename`, `Move`, `Delete`; include `Show all`/`Hide all` only when the item has `visible`.
- Interface mapper dispatches by concrete type.

Create menu:
- If the feature already uses `FloatingActionButtonMenu`, add group creation there.
- Prefer a FAB menu over a picker popup when there are multiple create actions.

## Pickers and Move

Group picker pattern:

```kotlin
val loader = ThingGroupLoader(getAppService<ThingService>().loader)
val manager = GroupListManager(scope, loader, null, augment = {
    filter(it.filterIsInstance<ThingGroup>())
})
GroupablePickers.group(..., manager, ThingGroupMapper(context) { _, _ -> }, ...)
```

Move command:
- Open the group picker with `initialGroup = value.parentId`.
- If moving a group, filter out itself at minimum.
- Update `parentId` via `copy(parentId = selectedGroup?.id)`.
- Toast `R.string.moved_to` with selected group name or `R.string.no_group`.

## Validation

After implementation:
- Run `./gradlew :app:compileDebugKotlin`.
- Scan for stale direct singleton calls if a new service/repo is registered with the tool singleton list.
- Scan migrations for missing entity indices.
