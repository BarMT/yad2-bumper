//> using toolkit latest

package com.burdinov.yad2


import sttp.client4.quick.*
import sttp.client4.Response

import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant, ZoneId, ZonedDateTime}
import java.io.{FileWriter, BufferedWriter, File}



val defaultSleep = 7200000L //2 hours
val graceSeconds = 5

val logFile = os.pwd / "yad2.log"

val PATTERN_FORMAT = "dd.MM.yyyy HH:mm:ss"
val formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT)
val localZone = ZoneId.of("Asia/Jerusalem")


/**
 * This script will continuously bump all of the user's ads on Yad2.co.il second hand market
 * @param args - first argument must be email and the second must be password
 */
@main def main(args: String*): Unit =
  println("Yad2 Bumper. ver 1.0")

  if (args.size != 2)
    println("required parameters: <email> <password>")
    System.exit(1)
  
  val Seq(email, pass) = args


  while (true) {
    val authCookie = login(email, pass)
    if (!authCookie.code.isSuccess)
      log("login failed.")
      System.exit(1)

    log("logged in")

    val items = getAllItems(authCookie)
    log(s"queried ${items.size} items")

    if (items.isEmpty)
      log(s"Nothing to bump. sleeping.")
      Thread.sleep(defaultSleep)
    else 
      val nextBumpAt = items.map(itemId => promoteItem(itemId, authCookie)).max
      log(s"bumped ${items.size} items. next bump at: ${formatter.format(nextBumpAt)}")

      val timeNow = Instant.now.atZone(localZone)
      Thread.sleep(Duration.between(timeNow, nextBumpAt.plusSeconds(graceSeconds)).toMillis)
  }

def login(email: String, pass: String): Response[String] =
  val loginCreds = ujson.Obj(
    "email" -> email,
    "password" -> pass
  )

  quickRequest
    .post(uri"https://gw.yad2.co.il/auth/login")
    .header("Content-Type", "application/json")
    .body(ujson.write(loginCreds))
    .send()


def getAllItems(authCookie: Response[_]): Seq[Long] =
  val response = quickRequest
    .get(uri"https://gw.yad2.co.il/my-ads/?filterByStatuses[]=APPROVED&filterByStatuses[]=DEACTIVATED&filterByStatuses[]=REVIEWING&filterByStatuses[]=REFUSED&filterByStatuses[]=EXPIRED&filterByStatuses[]=REFUSED_DUE_TO_BUSINESS_AD&filterByStatuses[]=AWAITING_CONFIRMATION_BY_PHONE&filterByStatuses[]=REFUSED_DUE_TO_SUSPECTED_BUSINESS_CUSTOMER&filterByStatuses[]=AWAITING_CONFIRMATION_BY_KENNEL_CLUB&filterByStatuses[]=IN_PROGRESS_BEFORE_FINISH&filterByStatuses[]=AWAITING_PAYMENT_BY_PHONE&filterByStatuses[]=NEW_AD_NOT_PUBLISHED&filterByStatuses[]=AWAITING_COMMERCIAL_PAYMENT&filterByStatuses[]=WAITING_FOR_PHONE_APPROVAL&filterByStatuses[]=BUSINESS_TREATMENT&page=1")
    .header("Content-Type", "application/json")
    .cookies(authCookie)
    .send()

  ujson.read(response.body)("data")("items").arr.map(_.obj("id").num.toLong).toSeq

def promoteItem(itemId: Long, authCookie: Response[_]): ZonedDateTime =
  val response = quickRequest
    .put(uri"https://gw.yad2.co.il/my-ads/$itemId/promotion")
    .header("Content-Type", "application/json")
    .cookies(authCookie)
    .send()

  val dateTxt = ujson.read(response.body)("data")("allowManualPromotionAfter").str
  Instant.parse(dateTxt).atZone(localZone)

def log(msg: String): Unit =
  val str = s"${formatter.format(Instant.now.atZone(localZone))} - $msg\n"
  print(str)
  os.write.append(logFile, str)
  