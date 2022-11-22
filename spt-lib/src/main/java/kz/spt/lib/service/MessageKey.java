package kz.spt.lib.service;

public interface MessageKey {

    public static final String MANUAL_CLOSE_IN = "events.message.manual.close.IN";
    public static final String MANUAL_CLOSE_OUT = "events.message.manual.close.OUT";
    public static final String MANUAL_CLOSE = "events.message.manual.close";
    public static final String MANUAL_OPEN_IN = "events.message.manual.open.IN";
    public static final String MANUAL_OPEN_OUT = "events.message.manual.open.OUT";
    public static final String MANUAL_OPEN = "events.message.manual.open";
    public static final String MANUAL_RESTART = "events.message.manual.restart";
    public static final String REASON = "events.message.reason";
    public static final String PASS = "events.message.pass";
    public static final String ERROR_BARRIER_CANNOT_ASSIGN_VALUE = "events.message.error.barrier.cannotAssignValue";
    public static final String ERROR_BARRIER_CANNOT_ASSIGN_VALUE_IN = "events.message.error.barrier.cannotAssignValue.IN";
    public static final String ERROR_BARRIER_CANNOT_ASSIGN_VALUE_OUT = "events.message.error.barrier.cannotAssignValue.OUT";
    public static final String ABONNEMENT_EXPIRED = "events.message.abonnement.expired";
    public static final String WARNING_EXPIRATION = "events.message.warning.expiration";
    public static final String MANUAL_PASS_IN = "events.message.manual.pass.IN";
    public static final String MANUAL_PASS_OUT = "events.message.manual.pass.OUT";
    public static final String MANUAL_PASS = "events.message.manual.pass";
    public static final String MANUAL_PASS_WITHOUT_OPEN = "events.message.manual.pass.withoutOpenGate";
    public static final String MANUAL_PASS_WITHOUT_OPEN_OUT= "events.message.manual.pass.withoutOpenGate.OUT";
    public static final String MANUAL_PASS_WITHOUT_OPEN_IN = "events.message.manual.pass.withoutOpenGate.IN";
    public static final String ERROR_BARRIER_NON_WORKING_HOURS = "events.message.error.barrier.nonWorkingHours";
    public static final String NEW_CAR_AT_NIGHT = "events.message.new.car.atNight";
    public static final String NOT_ALLOWED_BAN = "events.message.notAllowed.ban";
    public static final String NOT_ALLOWED_DEBT = "events.message.notAllowed.debt";
    public static final String ALLOWED_EXISTS_SUM_FOR_PREPAY = "events.message.allowed.payment.existSumForPrepay";
    public static final String NOT_ALLOWED_NOT_ENOUGH_BALANCE = "events.message.notAllowed.notEnoughBalance";
    public static final String ENTRANCE_ALLOWED = "events.message.entrance.allowed";
    public static final String ENTRANCE_CANCEL = "events.message.entrance.cancel";
    public static final String ALLOWED_VALID_PAID_PERMIT = "events.message.allowed.payment.validPaidPermit";
    public static final String ALLOWED_PAYMENT_PREPAID_BASIS = "events.message.allowed.payment.prepaidBasis";
    public static final String ALLOWED_PAID_BASIS = "events.message.allowed.payment.paidBasis";
    public static final String ALLOWED_FREE_PERMIT = "events.message.allowed.payment.freePermit";
    public static final String ALLOWED_PREPAID_BASIS_WITHOUT_DIMENSION = "events.message.allowed.payment.prepaidBasisWithoutDimension";
    public static final String ALLOWED_PAID_BASIS_WITHOUT_DIMENSION = "events.message.allowed.payment.paidBasisWithoutDimension";
    public static final String ALLOWED_FREE_PERMIT_WITHOUT_DIMENSION = "events.message.allowed.payment.freePermitWithoutDimension";
    public static final String ALLOWED_PAID_BASIS_BY_GROUP = "events.message.allowed.payment.paidBasis.byGroup";
    public static final String ALLOWED_PAID_BASIS_BY_GROUP_ALL = "events.message.allowed.payment.paidBasis.byGroup.all";
    public static final String ALLOWED_VALID_BOOKING = "events.message.allowed.payment.validBooking";
    public static final String NOT_ALLOWED_ALL_SPOTS_TAKEN = "events.message.notAllowed.allSpotsTakenByGroup";
    public static final String NOT_ALLOWED_NO_ACCESS_ENTER = "events.message.notAllowed.noAccess.enter";
    public static final String NOT_ALLOWED_NOT_FOUND_IN_FREE_PERMIT = "events.message.notAllowed.notFound.notInFreePermit";
    public static final String NOT_FOUND_RECORD_ALLOWED_BY_FREE_PERMIT = "events.message.notFound.record.allowed.byFreePermit";
    public static final String NOT_FOUND_RECORD_NOT_ALLOWED = "events.message.notFound.record.notAllowed";
    public static final String ALLOWED_PREPAID = "events.message.allowed.payment.prepaid";
    public static final String NOT_FOUND_RECORD_ALLOWED = "events.message.notFound.record.allowed";
    public static final String NOT_FOUND_RECORD_NOT_ALLOWED_PAID_PARKING = "events.message.notFound.record.notAllowed.paidParking";
    public static final String NOT_ALLOWED_NO_ACCESS_EXIT = "events.message.notAllowed.noAccess.exit";
    public static final String NOT_ALLOWED_NOT_ENOUGH_BALANCE_NOT_IN_FREE_PERMIT = "events.message.notAllowed.notEnoughBalance.outsideOfFreePermits";
    public static final String NOT_ALLOWED_NOT_ENOUGH_BALANCE_NOT_IN_PAID_PERMIT = "events.message.notAllowed.notEnoughBalance.outsideOfPaidPermits";
    public static final String ERROR_CALCULATION = "events.message.error.calculation";
    public static final String NOT_ALLOWED_NOT_ENOUGH_BALANCE_TO_PAY = "events.message.notAllowed.notEnoughBalance.toPay";
    public static final String NOT_FOUND_PLUGIN_BALANCE = "events.message.notFound.plugin.balance";
    public static final String EXIT_CANCEL = "events.message.exit.cancel";
    public static final String ERROR_BARRIER_COULD_NOT_READ_STATE = "events.message.error.barrier.couldNotReadState";
    public static final String ERROR_BARRIER_COULD_NOT_READ_STATE_OUT = "events.message.error.barrier.couldNotReadState.OUT";
    public static final String ERROR_BARRIER_COULD_NOT_READ_STATE_IN = "events.message.error.barrier.couldNotReadState.IN";
    public static final String ALLOWED_WHITELIST = "events.message.allowed.whitelist";
    public static final String ALLOWED_PREPAID_BASIS = "events.message.allowed.prepaidBasis";
    public static final String ALLOWED_PAYMENT_THIRD_PARTY = "events.message.allowed.payment.thirdParty";
    public static final String ALLOWED = "events.message.allowed";
    public static final String NOT_FOUND_ENTERING = "events.message.notFound.entering";
    public static final String ALLOWED_PAYMENT_ZERO_TOUCH = "events.message.allowed.payment.zeroTouch";
    public static final String ALLOWED_PAYMENT_PAID = "events.message.allowed.payment.paid";
    public static final String ALLOWED_FREE_MINUTES = "events.message.allowed.payment.freeMinutes";
    public static final String ALLOWED_NO_PAYMENT_REQUIRED = "events.message.allowed.payment.noPaymentRequired";
    public static final String NEW_LICENCE_PLATE = "events.message.new.licencePlate";
    public static final String NOT_FOUND_PLUGIN_CALCULATE_TOTAL_SUM = "events.message.notFound.plugin.calculatingTotalSum";
    public static final String ALLOWED_WITH_DEBT = "events.message.allowed.payment.withDebt";
    public static final String NEW_CAR_FROM_UNKNOWN_CAMERA = "events.message.new.car.fromUnknownCamera";
    public static final String NOT_FOUND_PLUGIN_BILLING = "events.message.notFound.plugin.billing";
    public static final String NOT_FOUND_PLUGIN_RATE = "events.message.notFound.plugin.rate";

    public static final String DIMENSION_PASSENGER_CAR = "events.dimension.passengerCar";
    public static final String DIMENSION_TRUCK = "events.dimension.truck";
    public static final String DIMENSION_GAZELLE = "events.dimension.gazelle";
    public static final String DIMENSION_NOT_RECOGNIZED = "events.dimension.notRecognized";
    public static final String DIMENSION_MINIBUS = "events.dimension.minibus";
    public static final String BARRIER_SEND_SIGNAL_OPEN_IN = "events.message.barrier.send.signal.open.IN";
    public static final String BARRIER_SEND_SIGNAL_OPEN_OUT = "events.message.barrier.send.signal.open.OUT";
    public static final String BARRIER_SEND_SIGNAL_OPEN = "events.message.barrier.send.signal.open";
    public static final String BARRIER_SEND_SIGNAL_EXIT_IN = "events.message.barrier.send.signal.exit.IN";
    public static final String BARRIER_SEND_SIGNAL_EXIT_OUT = "events.message.barrier.send.signal.exit.OUT";
    public static final String BARRIER_SEND_SIGNAL_EXIT = "events.message.barrier.send.signal.exit";
    public static final String BARRIER_GET_SIGNAL_OPEN_IN = "events.message.barrier.get.signal.open.IN";
    public static final String BARRIER_GET_SIGNAL_OPEN_OUT = "events.message.barrier.get.signal.open.OUT";
    public static final String BARRIER_GET_SIGNAL_OPEN = "events.message.barrier.get.signal.open";
    public static final String BARRIER_COULD_NOT_CHANGE = "events.message.barrier.couldNotChange";
    public static final String BARRIER_COULD_NOT_CHANGE_IN = "events.message.barrier.couldNotChange.IN";
    public static final String BARRIER_COULD_NOT_CHANGE_OUT = "events.message.barrier.couldNotChange.OUT";
    public static final String HAVE_PLATENUMBER = "events.message.platenumber";
    public static final String MANUAL_PASS_WITH_DEBT = "events.message.manual.pass.debt";
    public static final String JOURNAL_MANUAL_EDIT_NUMBER = "events.message.journal.manual.edit.number";
    public static final String BILLING_PREPAID_NOT_FOUND = "billing.message.error.notFound.prepaid";
    public static final String BILLING_ERROR_INCORRECT_PLATENUMBER = "billing.message.error.incorrect.platenumber";
    public static final String BILLING_ERROR_NULL_TXN = "billing.message.error.null.txn";
    public static final String BILLING_ERROR_INCORRECT_SUM = "billing.message.error.incorrect.sum";
    public static final String BILLING_NOT_ALLOWED_PAYMENT = "billing.message.error.notAllowed.payment";
    public static final String BILLING_PAYMENT_PARKING = "billing.message.payment.parking";
    public static final String BILLING_NOT_FOUND_PARKING = "billing.message.error.notFound.parking";
    public static final String BILLING_PAYMENT_PAID_PERMIT = "billing.message.payment.paidPermit";
    public static final String BILLING_PAYMENT_DEBT = "billing.message.payment.debt";
    public static final String MANUAL_EDIT_DIMENSION = "events.message.manual.edit.dimension";
    public static final String BILLING_REASON_PAYMENT_PAID_PERMIT = "billing.reason.payment.paidPermit";
    public static final String BILLING_DESCRIPTION_RECEIVED_FROM_PROVIDER = "billing.description.payment.received.provider";
    public static final String BILLING_REASON_PAYMENT_PARKING = "billing.reason.payment.payment.parking";
    public static final String BILLING_REASON_DEBT_CANCEL = "billing.reason.debt.cancel";
    public static final String BILLING_REASON_MANUAL_TOP_UP = "billing.reason.manual.topUp.byUser";
    public static final String BILLING_REASON_MANUAL_WRITE_OFF = "billing.reason.manual.writeOff.byUser";
    public static final String ABONNEMENT_ERROR_DATES_OVERLAP_PLATENUMBER = "abonoment.error.dates.overlap.platenumber";
    public static final String SYMBOLS_DAY = "symbols.day";
    public static final String SYMBOLS_HOUR = "symbols.hour";
    public static final String SYMBOLS_MINUTE = "symbols.minute";
    public static final String SYMBOLS_SECOND = "symbols.second";
    public static final String ABONNEMENT_TYPE_ALL_DAYS = "abonnementType.allDays";
    public static final String ABONNEMENT_TYPE_FOR_DAYS_CUSTOM = "abonnementType.forDays.custom";
    public static final String ABONNEMENT_TYPE_FOR_DAYS = "abonnementType.forDays";
    public static final String MONDAY = "day.monday";
    public static final String TUESDAY = "day.tuesday";
    public static final String WEDNESDAY = "day.wednesday";
    public static final String THURSDAY = "day.thursday";
    public static final String FRIDAY = "day.friday";
    public static final String SATURDAY = "day.saturday";
    public static final String SUNDAY = "day.sunday";
}
