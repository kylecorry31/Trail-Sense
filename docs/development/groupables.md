# Groupable items
Steps to make an item groupable
1. Create interface for the item, implementing IGroupable
2. Create a group class for the item, implementing the same interface as the item 
3. Add the parentId to the item's DB entity
4. Add a DB entity for the groupable class and set up the Dao for it and Dao accessors for the item (get/delete in group)
5. Add accessor methods to the item's repo for group operations - in addition to normal operations, getGroupsWithParent and getItemsWithParent methods are needed
6. In the feature's service, set up a GroupLoader, GroupCounter, and GroupDeleter
7. Create a class which inherits from ISearchableGroupLoader
8. Add list mappers for the item interface, and group (may need to set up GroupMappers for displayed fields)
9. Update the item's list to use the interface mappers
10. Use the searchable group loader class to populate the UI (use GroupListManager, and setup it's onChange, refresh, and up calls)
11. Add group/item pickers
12. Add support for creating a group
13. Add support for moving items between groups
14. Add support for deleting a group
15. Add support for renaming a group