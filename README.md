# Quick Leaf Decay
get it on [Modrinth](https://modrinth.com/mod/quickleafdecay)\
![video preview](https://github.com/wb1016/QuickLeafDecay/assets/47267045/089de74c-f6ba-4383-bf94-56784709308f)\
Minecraft Mod for Fabric/Quilt loader. **requires Fabric API or QSL**.\
it started as a fork of [Leaves Us In Peace](https://modrinth.com/mod/leaves-us-in-peace) for latest version, but i decided to clean up the dependencies to make it sustainable.
## What it does
all of features are configurable under config folder of your minecraft instance.
### by default
- Leaves will decay much faster. This is basically like the fast leaf decay mods you've likely seen before. It's highly configurable, and uses an algorithm that can check for diagonal leaves, in addition to the usual check for adjacent leaves, so it's less likely to leave a stray leaf or two floating.
- This is how the mod handles azaleas' two types of leaves. Mods (or mod users) can add compat for trees with multiple types of leaves by just adding a tag. Details on the [wiki](https://gitlab.com/supersaiyansubtlety/leaves_us_in_peace/-/wikis/home).
### optional
- Leaves will ignore leaves of a different type when determining whether to decay or not.
- Leaves will ignore logs from different tree types when determining whether to decay or not. This will look for a tag by the name of the log and check if it contains the leaves doing the check. If no tag is found, any log will match instead (like in vanilla). Details on the wiki.
## documentation
check [Leaves Us In Peace wiki](https://gitlab.com/supersaiyansubtlety/leaves_us_in_peace/-/wikis/home)
