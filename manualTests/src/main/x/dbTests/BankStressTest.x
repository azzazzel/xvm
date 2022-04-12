/**
 * This is a test app for the Bank demo.
 *
 *  To prepare the test, follow steps 1-5 outlined in TestSimpleWeb.
 *
 *  To run the test:
 *      curl -i -w '\n' -X GET http://[domain]:8080/run
 *
 * See [Bank] database module.
 */
@web.WebModule
module BankStressTest
    {
    package web import web.xtclang.org;

    package Bank import Bank;

    import Bank.Account;
    import Bank.Connection;

    import Bank.oodb.DBClosed;
    import Bank.oodb.CommitFailed;

    static Duration TEST_DURATION = Duration:30s;
    static Int      BRANCHES      = 24;
    static Int      MAX_ACCOUNTS  = 100;

    @web.WebService
    service Test
        {
        @web.Get("/run")
        String run()
            {
            Branch[] branches = new Branch[BRANCHES](i -> new Branch(i.toUInt64()));
            for (Branch branch : branches)
                {
                branch.doBusiness^(TEST_DURATION);
                }

            private void checkOpen(Branch[] branches, Timer timer)
                {
                for (Branch branch : branches)
                    {
                    // wait until all branches are closed
                    if (branch.status == Open)
                        {
                        timer.schedule(Duration.ofSeconds(10), &checkOpen(branches, timer));
                        return;
                        }
                    }
                @Inject Connection bank;
                bank.log.add("All branches have closed");
                }

            // schedule a periodic check
            @Inject Timer timer;
            timer.schedule(Duration.ofSeconds(10), &checkOpen(branches, timer));
            return "Bank is open";
            }
        }

    service Branch(UInt branchId)
        {
        @Inject Connection bank;
        @Inject Clock      clock;

        enum Status {Initial, Open, Closed}
        public/private Status status = Initial;

        void doBusiness(Duration duration)
            {
            Int      tryCount = 0;
            Int      txCount  = 0;
            DateTime start    = clock.now;
            DateTime close    = start + duration;
            Random   rnd      = new ecstasy.numbers.PseudoRandom(branchId);

            status = Open;
            bank.log.add($"Branch {branchId} opened");

            business:
            while (True)
                {
                if (++tryCount % 100 == 0)
                    {
                    DateTime now = clock.now;
                    if (now < close)
                        {
                        bank.log.add(
                            $|Branch {branchId} performed {100 + txCount} transactions in \
                             |{(now - start).seconds} seconds
                             );
                        txCount = 0;
                        start   = now;
                        }
                    else
                        {
                        break business;
                        }
                    }

                String op = "";
                try
                    {
                    Int acctId = rnd.int(MAX_ACCOUNTS);
                    switch (rnd.int(100))
                        {
                        case 0..1:
                            op = "OpenAccount";
                            if (!bank.accounts.contains(acctId))
                                {
                                txCount++;
                                bank.openAccount(acctId, 256_00);
                                }
                            break;

                        case 2..3:
                            op = "CloseAccount";
                            if (bank.accounts.contains(acctId))
                                {
                                txCount++;
                                bank.closeAccount(acctId);
                                }
                            break;

                        case 4..49:
                            op = "Deposit or Withdrawal";
                            if (Account acc := bank.accounts.get(acctId))
                                {
                                txCount++;
                                Int amount = rnd.boolean() ? acc.balance/2 : -acc.balance/2;
                                bank.depositOrWithdraw(acctId, amount);
                                }
                            break;

                        case 50..98:
                            op = "Transfer";
                            Int acctIdTo = rnd.int(MAX_ACCOUNTS);
                            if (acctIdTo != acctId,
                                    Account accFrom := bank.accounts.get(acctId),
                                    bank.accounts.contains(acctIdTo),
                                    accFrom.balance > 100)
                                {
                                txCount++;
                                bank.transfer(acctId, acctIdTo, accFrom.balance / 2);
                                }
                            break;

                        case 99:
                            op = "Audit";
                            txCount++;
                            bank.log.add($"Branch {branchId} audited amount is {Bank.format(bank.audit())}");
                            break;
                        }
                    }
                catch (Exception e)
                    {
                    bank.log.add($"{op} failed at {branchId}: {e.text}");
                    if (op == "Audit"
                            || e.is(DBClosed)
                            || e.is(CommitFailed) && e.result == DatabaseError
                            )
                        {
                        break business;
                        }
                    }
                }

            bank.log.add($"Branch {branchId} closed");
            status = Closed;
            }
        }
    }