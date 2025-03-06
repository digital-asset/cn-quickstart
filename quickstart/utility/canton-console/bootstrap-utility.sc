import com.digitalasset.canton.console.ParticipantReference
import com.digitalasset.canton.topology.PartyId

def checkPackageIsVetted(participant: ParticipantReference, packageName: String) : Unit = {
  val packageId = participant.packages.list().filter(_.sourceDescription.contains(packageName.dropRight(4))).map(_.packageId)
  val vettedPackages = participant.topology.vetted_packages.list().flatMap(_.item.packages.collect(_.packageId))
  if(packageId.size == 0 || !vettedPackages.exists(_ == packageId.head)) {
    Thread.sleep(1000)
    checkPackageIsVetted(participant,packageName)
  }
}

def createUserWithParty(participant: ParticipantReference, name: String): (String, PartyId) = {
  val party = participant.ledger_api.parties.allocate(name, name).party
  participant.ledger_api.users.create(name, Set(party), Some(party), Set(party))
  (name, party)
}

def createUtilityAdminUser(participant: ParticipantReference, name: String, parties: Set[PartyId]) = {
  participant.ledger_api.users.create(name, parties, None, parties, true)
}

val dars = sys.env("UTILITY_DARS").split(",").map(_.trim)
val operatorUser = sys.env("UTILITY_OPERATOR")
val users = sys.env("UTILITY_USERS").split(",").toList
val utilityAdminUser = sys.env("UTILITY_ADMIN_USER")

// Upload all DARs to the participants
logger.info(s"UTILITY-BOOTSTRAP: Uploading DARs to participants...")
for (d <- dars){
  logger.info(s"UTILITY-BOOTSTRAP: Uploading provider DAR $d")
  providerParticipant.dars.upload(s"/dars/$d")
  checkPackageIsVetted(providerParticipant,d)
}
for (d <- dars){
  logger.info(s"UTILITY-BOOTSTRAP: Uploading user DAR $d")
  userParticipant.dars.upload(s"/dars/$d")
  checkPackageIsVetted(userParticipant,d)
}

logger.info(s"UTILITY-BOOTSTRAP: Creating users and parties...")
val operatorParty = createUserWithParty(providerParticipant, operatorUser)
val userParties = users.map(user => createUserWithParty(userParticipant, user))

logger.info(s"UTILITY-BOOTSTRAP: Granting act as rights to ledger-api-user...")
createUtilityAdminUser(providerParticipant, utilityAdminUser, Set(operatorParty._2))
createUtilityAdminUser(userParticipant, utilityAdminUser, (userParties.map(_._2).toSet))

utils.generate_daml_script_participants_conf(
    Some("/output/participant-config.json"),
    useParticipantAlias = true,
    defaultParticipant = Some(providerParticipant)
  )
