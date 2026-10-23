package provider

import editor.objects.GenericGdObject

interface ObjectManager<Obj : GenericGdObject>: ScopedObjectAdder<Obj>, GroupHandler