package org.mozilla.rocket.urlinput

data class SearchPortal(var name: String, var icon: String, var searchUrlPattern: String, var homeUrl: String) {

    override fun toString(): String {
        return "SearchPortal{" +
                "name='" + name + '\'' +
                ", icon=" + icon + '\'' +
                ", searchUrlPattern='" + searchUrlPattern + '\'' +
                ", homeUrl='" + homeUrl + '\'' +
                '}'
    }
}
