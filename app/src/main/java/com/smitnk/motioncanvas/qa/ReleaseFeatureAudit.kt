package com.smitnk.motioncanvas.qa
data class FeatureAudit(val name:String,val enginePresent:Boolean,val uiWired:Boolean,val buildVerified:Boolean)
object ReleaseFeatureAudit{
 fun summary(features:List<FeatureAudit>)=features.groupBy{
  when{!it.enginePresent->"MISSING ENGINE";!it.uiWired->"ENGINE ONLY";!it.buildVerified->"WIRED/UNVERIFIED";else->"VERIFIED"}
 }
}